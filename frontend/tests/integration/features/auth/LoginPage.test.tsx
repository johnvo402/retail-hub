import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it } from "vitest";
import { authStore } from "@/lib/auth/authStore";
import LoginPage from "@/features/auth/LoginPage";

describe("LoginPage", () => {
  beforeEach(() => authStore.clear());

  it("shows accessible validation beside required fields", async () => {
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    const signInButtons = screen.getAllByRole("button", { name: "Sign in" });
    fireEvent.click(signInButtons[signInButtons.length - 1]);

    expect(await screen.findByText("Enter a valid email address")).toBeInTheDocument();
    expect(await screen.findByText("Password must contain at least 8 characters")).toBeInTheDocument();
  });

  it("supports switching to account registration", () => {
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    fireEvent.click(screen.getByRole("button", { name: "Register" }));
    expect(screen.getByRole("heading", { name: "Create your account" })).toBeInTheDocument();
  });
});
