import React from 'react';
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
    <div className="space-y-8 crt-effect">
      <div className="border-b-[4px] border-[#39ff14] pb-4 inline-block">
        <div className="h-10 w-48 bg-[#112240] animate-pulse border-2 border-[#39ff14]"></div>
      </div>
      <div className="retro-table-container">
        <table className="w-full retro-table">
          <thead className="bg-[#03040c]">
            <tr>
              <th>ORDER ID</th><th>CUSTOMER</th><th>ITEMS</th><th>TOTAL</th><th>STATUS</th>
            </tr>
          </thead>
          <tbody>
            {[...Array(4)].map((_, i) => (
              <tr key={i} className="animate-pulse">
                <td><div className="h-4 bg-[#00ffcc]/20 w-16"></div></td>
                <td><div className="h-4 bg-white/20 w-32"></div></td>
                <td>
                  <div className="space-y-2">
                    <div className="h-8 bg-[#ffea00]/20 w-48"></div>
                  </div>
                </td>
                <td><div className="h-6 bg-[#39ff14]/20 w-24"></div></td>
                <td><div className="h-6 bg-[#ff00ff]/20 w-20"></div></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
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
                <td colSpan="5" className="p-16 text-center border-t border-[#00ffcc]/30">
                  <div className="flex flex-col items-center justify-center space-y-4">
                    <ShoppingCart size={48} className="text-[#ff00ff] animate-pulse mb-2" />
                    <div className="text-[#ff00ff] font-bold uppercase tracking-widest text-xl">
                      NO ORDERS FOUND
                    </div>
                    <p className="text-[#00ffcc] opacity-70 font-mono text-sm max-w-md text-center">
                      The transaction ledger is currently empty. Awaiting new customer orders...
                    </p>
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
