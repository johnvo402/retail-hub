import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft } from "@phosphor-icons/react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { Link, Navigate, useNavigate, useParams } from "react-router-dom";
import { z } from "zod";
import { ErrorState, LoadingState } from "../../components/States";
import { problemMessage } from "../../lib/api/client";
import { useAuthState } from "../../lib/auth/authStore";
import { createCategory, getCategory, updateCategory } from "./categoryApi";

const schema = z.object({
  name: z.string().trim().min(1, "Name is required").max(120, "Name must be 120 characters or fewer"),
  description: z.string().max(500, "Description must be 500 characters or fewer"),
  active: z.boolean(),
});

type CategoryForm = z.infer<typeof schema>;

export default function CategoryFormPage() {
  const { id } = useParams();
  const editing = !!id;
  const auth = useAuthState();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const category = useQuery({
    queryKey: ["category", id],
    queryFn: () => getCategory(id!),
    enabled: editing && auth.user?.role === "ADMIN",
  });
  const { register, handleSubmit, reset, formState: { errors } } = useForm<CategoryForm>({
    resolver: zodResolver(schema),
    mode: "onBlur",
    defaultValues: { name: "", description: "", active: true },
  });

  useEffect(() => {
    if (category.data) {
      reset({
        name: category.data.name,
        description: category.data.description ?? "",
        active: category.data.active,
      });
    }
  }, [category.data, reset]);

  const save = useMutation({
    mutationFn: async (values: CategoryForm) => {
      if (editing) {
        await updateCategory(id!, values);
        return id!;
      }
      return (await createCategory(values)).id;
    },
    onSuccess: async (categoryId) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["categories"] }),
        queryClient.invalidateQueries({ queryKey: ["category", categoryId] }),
      ]);
      navigate("/categories/manage");
    },
  });

  if (auth.user?.role !== "ADMIN") return <Navigate to="/products" replace />;
  if (editing && category.isLoading) return <LoadingState label="Loading category" />;
  if (editing && category.isError) return <ErrorState message={problemMessage(category.error)}
    onRetry={() => void category.refetch()} />;

  return <div className="page-stack form-page">
    <Link to="/categories/manage" className="back-link"><ArrowLeft size={17} aria-hidden="true" /> Cancel</Link>
    <header className="page-header">
      <div><p className="eyebrow">Category editor</p>
        <h1>{editing ? "Edit category" : "New category"}</h1>
        <p className="page-description">Keep category names, descriptions, and availability accurate.</p>
      </div>
    </header>
    {save.isError && <div className="form-alert" role="alert">{problemMessage(save.error)}</div>}
    <form className="panel form-panel" onSubmit={handleSubmit((values) => save.mutate(values))} noValidate>
      <div className="form-grid">
        <div className="field field-wide">
          <label htmlFor="name">Category name <span aria-hidden="true">*</span></label>
          <input id="name" maxLength={120} {...register("name")} aria-invalid={!!errors.name}
            aria-describedby={errors.name ? "name-error" : undefined} autoFocus />
          {errors.name && <span id="name-error" className="field-error" role="alert">{errors.name.message}</span>}
        </div>
        <div className="field field-wide">
          <label htmlFor="description">Description</label>
          <textarea id="description" rows={5} maxLength={500} {...register("description")}
            aria-invalid={!!errors.description} aria-describedby={errors.description ? "description-error" : undefined} />
          {errors.description && <span id="description-error" className="field-error" role="alert">
            {errors.description.message}
          </span>}
        </div>
        <label className="check-field field-wide">
          <input type="checkbox" {...register("active")} />
          <span><strong>Active category</strong>
            <small>Active categories are available when assigning products.</small></span>
        </label>
      </div>
      <div className="form-actions">
        <Link className="button button-ghost" to="/categories/manage">Cancel</Link>
        <button className="button button-primary" type="submit" disabled={save.isPending}>
          {save.isPending ? "Saving…" : editing ? "Save changes" : "Create category"}
        </button>
      </div>
    </form>
  </div>;
}
