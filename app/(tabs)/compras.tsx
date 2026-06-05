import { CarritoScreen } from '@/presentation/screens/CarritoScreen';
import { useCompraCarritoStore } from '@/presentation/stores/carritoStore';

export default function ComprasTab() {
  return <CarritoScreen tipo="COMPRA" store={useCompraCarritoStore} />;
}
