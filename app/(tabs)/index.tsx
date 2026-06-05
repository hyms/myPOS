import { CarritoScreen } from '@/presentation/screens/CarritoScreen';
import { useVentaCarritoStore } from '@/presentation/stores/carritoStore';

export default function VentasTab() {
  return <CarritoScreen tipo="VENTA" store={useVentaCarritoStore} />;
}
