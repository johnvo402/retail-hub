import { afterAll, beforeEach, describe, expect, it, vi } from "vitest";
import { api } from "@/lib/api/client";
import { getDashboardOverview } from "@/features/dashboard/dashboardApi";

const get = vi.spyOn(api, "get");

describe("dashboardApi", () => {
  beforeEach(() => {
    get.mockReset();
    get.mockResolvedValue({ data: { activeProductCount: 0 } });
  });

  afterAll(() => get.mockRestore());

  it("loads the overview from the dedicated endpoint only", async () => {
    await getDashboardOverview();

    expect(get).toHaveBeenCalledOnce();
    expect(get).toHaveBeenCalledWith("/dashboard/overview");
  });
});
