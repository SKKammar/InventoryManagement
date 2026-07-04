import { z } from "zod";

export const productSchema = z.object({
  sku: z.string().min(3, "SKU must be at least 3 characters").max(50),
  name: z.string().min(2, "Name must be at least 2 characters").max(255),
  description: z.string().optional(),
  price: z.coerce.number().min(0.01, "Price must be greater than 0"),
  stockQuantity: z.coerce.number().int().min(0, "Stock cannot be negative"),
  category: z.string().optional(),
});
