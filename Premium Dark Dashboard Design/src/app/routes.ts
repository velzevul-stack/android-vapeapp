import { createBrowserRouter } from "react-router";
import { HomeScreen } from "./components/screens/HomeScreen";
import { AcceptScreen } from "./components/screens/AcceptScreen";
import { SellScreen } from "./components/screens/SellScreen";
import { CabinetScreen } from "./components/screens/CabinetScreen";
import { ManagementScreen } from "./components/screens/ManagementScreen";
import { AppLayout } from "./components/AppLayout";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: AppLayout,
    children: [
      { index: true, Component: HomeScreen },
      { path: "accept", Component: AcceptScreen },
      { path: "sell", Component: SellScreen },
      { path: "cabinet/*", Component: CabinetScreen },
      { path: "management", Component: ManagementScreen },
    ],
  },
]);
