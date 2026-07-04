import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { ShoppingCart } from 'lucide-react';

export default function Orders() {
  const { data: orders = [], isLoading } = useQuery({
    queryKey: ['orders'],
    queryFn: async () => {
      const res = await axios.get('/api/orders');
      return res.data;
    }
  });

  if (isLoading) return (
    <div className="flex items-center gap-4 mt-10">
      <div className="w-4 h-4 bg-[#ffea00] animate-ping"></div>
      <div className="text-[#ffea00] font-bold text-xl uppercase tracking-widest">LOADING DATA...</div>
    </div>
  );

  return (
    <div className="space-y-8 crt-effect">
      <div className="border-b-[4px] border-[#39ff14] pb-4 inline-block">
        <h2 className="text-4xl font-bold text-[#ffea00] tracking-widest uppercase">
          ORDERS
        </h2>
        <p className="text-[#39ff14] mt-2 font-mono uppercase opacity-80 font-bold">
          &gt; View all order transactions
        </p>
      </div>

      <div className="retro-table-container">
        <table className="w-full retro-table">
          <thead className="bg-[#03040c]">
            <tr>
              <th>ORDER ID</th>
              <th>CUSTOMER</th>
              <th>ITEMS</th>
              <th>TOTAL</th>
              <th>STATUS</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.id}>
                <td className="text-[#00ffcc] font-bold">#{order.id}</td>
                <td className="font-bold text-white uppercase">{order.username}</td>
                <td>
                  <div className="space-y-2">
                    {order.items?.map((item, idx) => (
                      <div key={idx} className="text-xs bg-[#03040c] border border-[#ffea00]/30 p-2 inline-block mr-2">
                        <span className="text-[#ffea00] font-bold">{item.quantity}x</span> {item.productName} 
                        <br/>
                        <span className="text-[#ff00ff]">(@ ₹{item.unitPrice?.toLocaleString('en-IN', {minimumFractionDigits: 2})})</span>
                      </div>
                    ))}
                  </div>
                </td>
                <td className="text-[#39ff14] font-bold text-lg">₹{order.totalAmount?.toLocaleString('en-IN', {minimumFractionDigits: 2})}</td>
                <td>
                  <span className="bg-[#03040c] text-[#ff00ff] border-[2px] border-[#ff00ff] px-3 py-1 text-xs font-bold uppercase shadow-[2px_2px_0px_0px_#ff00ff]">
                    {order.status}
                  </span>
                </td>
              </tr>
            ))}
            {orders.length === 0 && (
              <tr>
                <td colSpan="5" className="p-12 text-center text-[#ff00ff] font-bold uppercase border-t border-[#00ffcc]/30">
                  <div className="flex flex-col items-center justify-center">
                    <ShoppingCart size={48} className="text-[#ff00ff]/50 mb-4" />
                    <p>NO ORDERS FOUND</p>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
