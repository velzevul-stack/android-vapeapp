export interface Product {
  id: string;
  brand: string;
  flavor: string;
  category: string;
  stock: number;
  buyPrice: number;
  sellPrice: number;
}

export const mockProducts: Product[] = [
  { id: "1", brand: "ELFBAR", flavor: "Watermelon Ice", category: "Одноразки", stock: 12, buyPrice: 350, sellPrice: 550 },
  { id: "2", brand: "ELFBAR", flavor: "Blueberry Raspberry", category: "Одноразки", stock: 0, buyPrice: 350, sellPrice: 550 },
  { id: "3", brand: "HQD", flavor: "Mango Ice", category: "Одноразки", stock: 15, buyPrice: 300, sellPrice: 500 },
  { id: "4", brand: "VAPORESSO", flavor: "Mesh Coil", category: "Комплектующие", stock: 25, buyPrice: 200, sellPrice: 350 },
  { id: "5", brand: "SMOK", flavor: "Nord Coils", category: "Комплектующие", stock: 8, buyPrice: 150, sellPrice: 280 },
  { id: "6", brand: "NASTY", flavor: "Bad Blood", category: "Жидкости", stock: 18, buyPrice: 400, sellPrice: 650 },
];

export const categories = ["Все", "Одноразки", "Жидкости", "Комплектующие"];
