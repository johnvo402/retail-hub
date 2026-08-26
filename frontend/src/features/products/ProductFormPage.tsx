import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, Plus } from "@phosphor-icons/react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Link, Navigate, useNavigate, useParams } from "react-router-dom";
import { z } from "zod";
import { ErrorState, LoadingState } from "../../components/States";
import { problemMessage } from "../../lib/api/client";
import { useAuthState } from "../../lib/auth/authStore";
import { createCategory, createProduct, getCategories, getProduct, updateProduct } from "./productApi";

const schema = z.object({
  name: z.string().trim().min(1, "Name is required").max(200),
  description: z.string().max(10000),
  sku: z.string().trim().min(1, "SKU is required").max(80),
  price: z.number({ error: "Enter a valid price" }).min(0, "Price cannot be negative"),
  categoryId: z.string().min(1, "Choose a category"),
  active: z.boolean(),
});
type ProductForm = z.infer<typeof schema>;

export default function ProductFormPage() {
  const { id } = useParams();
  const editing = !!id;
  const auth = useAuthState();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [categoryOpen, setCategoryOpen] = useState(false);
  const [categoryName, setCategoryName] = useState("");
  const [categoryDescription, setCategoryDescription] = useState("");
  const categories = useQuery({ queryKey: ["categories"], queryFn: getCategories });
  const product = useQuery({ queryKey: ["product", id], queryFn: () => getProduct(id!), enabled: editing });
  const { register, handleSubmit, reset, setValue, formState: { errors } } = useForm<ProductForm>({
    resolver: zodResolver(schema), mode: "onBlur",
    defaultValues: { name: "", description: "", sku: "", price: 0, categoryId: "", active: true },
  });

  useEffect(() => {
    if (product.data) reset({ name: product.data.name, description: product.data.description,
      sku: product.data.sku, price: product.data.price, categoryId: product.data.categoryId, active: product.data.active });
  }, [product.data, reset]);

  const save = useMutation({
    mutationFn: async (values: ProductForm) => {
      if (editing) {
        await updateProduct(id!, values);
        return { id: id! };
      }
      return createProduct(values);
    },
    onSuccess: async (result) => {
      await queryClient.invalidateQueries({ queryKey: ["products"] });
      navigate(`/products/${result.id}`);
    },
  });
  const addCategory = useMutation({ mutationFn: () => createCategory(categoryName, categoryDescription),
    onSuccess: async ({ id: categoryId }) => {
      await queryClient.invalidateQueries({ queryKey: ["categories"] });
      setValue("categoryId", categoryId, { shouldValidate: true });
      setCategoryOpen(false); setCategoryName(""); setCategoryDescription("");
    }});

  if (auth.user?.role !== "ADMIN") return <Navigate to="/products" replace />;
  if (editing && product.isLoading) return <LoadingState label="Loading product" />;
  if (editing && product.isError) return <ErrorState message="The product could not be loaded." />;

  return <div className="page-stack form-page">
    <Link to={editing ? `/products/${id}` : "/products"} className="back-link"><ArrowLeft size={17} /> Cancel</Link>
    <header className="page-header"><div><p className="eyebrow">Catalog editor</p>
      <h1>{editing ? "Edit product" : "New product"}</h1>
      <p className="page-description">Keep catalog details accurate and searchable.</p></div></header>
    {save.isError && <div className="form-alert" role="alert">{problemMessage(save.error)}</div>}
    <form className="panel form-panel" onSubmit={handleSubmit((values) => save.mutate(values))} noValidate>
      <div className="form-grid">
        <div className="field field-wide"><label htmlFor="name">Product name <span aria-hidden="true">*</span></label>
          <input id="name" {...register("name")} aria-invalid={!!errors.name} />
          {errors.name && <span className="field-error">{errors.name.message}</span>}</div>
        <div className="field"><label htmlFor="sku">SKU <span aria-hidden="true">*</span></label>
          <input id="sku" {...register("sku")} aria-invalid={!!errors.sku} autoCapitalize="characters" />
          {errors.sku && <span className="field-error">{errors.sku.message}</span>}</div>
        <div className="field"><label htmlFor="price">Price (USD) <span aria-hidden="true">*</span></label>
          <input id="price" type="number" min="0" step="0.01" {...register("price", { valueAsNumber: true })} aria-invalid={!!errors.price} />
          {errors.price && <span className="field-error">{errors.price.message}</span>}</div>
        <div className="field field-wide"><div className="label-row"><label htmlFor="categoryId">Category <span aria-hidden="true">*</span></label>
          <button type="button" className="text-button" onClick={() => setCategoryOpen((open) => !open)} aria-expanded={categoryOpen}>
            <Plus size={15} /> Add category</button></div>
          <select id="categoryId" {...register("categoryId")} aria-invalid={!!errors.categoryId}>
            <option value="">Select a category</option>{categories.data?.map((category) =>
              <option key={category.id} value={category.id}>{category.name}</option>)}
          </select>{errors.categoryId && <span className="field-error">{errors.categoryId.message}</span>}</div>
        {categoryOpen && <fieldset className="inline-creator field-wide"><legend>Create a category</legend>
          <div className="inline-fields"><label>Name<input value={categoryName} maxLength={120} onChange={(event) => setCategoryName(event.target.value)} /></label>
            <label>Description<input value={categoryDescription} maxLength={500} onChange={(event) => setCategoryDescription(event.target.value)} /></label>
            <button className="button button-secondary" type="button" disabled={!categoryName.trim() || addCategory.isPending}
              onClick={() => addCategory.mutate()}>{addCategory.isPending ? "Adding…" : "Add"}</button></div>
          {addCategory.isError && <span className="field-error" role="alert">{problemMessage(addCategory.error)}</span>}
        </fieldset>}
        <div className="field field-wide"><label htmlFor="description">Description</label>
          <textarea id="description" rows={5} {...register("description")} aria-invalid={!!errors.description} />
          {errors.description && <span className="field-error">{errors.description.message}</span>}</div>
        <label className="check-field field-wide"><input type="checkbox" {...register("active")} />
          <span><strong>Active product</strong><small>Active products appear in catalog browsing and search.</small></span></label>
      </div>
      <div className="form-actions"><Link className="button button-ghost" to={editing ? `/products/${id}` : "/products"}>Cancel</Link>
        <button className="button button-primary" type="submit" disabled={save.isPending}>{save.isPending ? "Saving…" : "Save product"}</button></div>
    </form>
  </div>;
}
