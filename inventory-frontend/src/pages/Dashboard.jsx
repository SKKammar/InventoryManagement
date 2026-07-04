import React from 'react';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { Package, ShoppingCart, AlertTriangle, Activity } from 'lucide-react';

export default function Dashboard() {
  const { data: products = [], isLoading: pLoading } = useQuery({
    queryKey: ['products'],
    queryFn: async () => {
      const res = await axios.get('/api/products');
      return res.data;
    }
  });

  const { data: orders = [], isLoading: oLoading } = useQuery({
    queryKey: ['orders'],
    queryFn: async () => {
      const res = await axios.get('/api/orders');
      return res.data;
    }
  });

  const totalValue = products.reduce((acc, p) => acc + (p.price * p.stockQuantity), 0);
  const lowStock = products.filter(p => p.stockQuantity < 10).length;

  if (pLoading || oLoading) return (
    <div className="flex items-center gap-4 mt-10">
      <div className="w-4 h-4 bg-[#ffea00] animate-ping"></div>
      <div className="text-[#ffea00] font-bold text-xl uppercase tracking-widest">LOADING DATA...</div>
    </div>
  );

  const stats = [
    { name: 'TOTAL PRODUCTS', value: products.length, icon: Package, color: '#00ffcc' },
    { name: 'ORDERS PROCESSED', value: orders.length, icon: ShoppingCart, color: '#ff00ff' },
    { name: 'LOW STOCK ALERTS', value: lowStock, icon: AlertTriangle, color: '#ffea00' },
    { name: 'INVENTORY VALUATION', value: `₹${totalValue.toLocaleString('en-IN', {minimumFractionDigits: 2})}`, icon: Activity, color: '#39ff14' },
  ];

  return (
    <div className="space-y-10 crt-effect">
      <div className="border-b-[4px] border-[#00ffcc] pb-4 inline-block">
        <h2 className="text-4xl font-bold text-[#ffea00] tracking-widest uppercase">
          DASHBOARD
        </h2>
        <p className="text-[#00ffcc] mt-2 font-mono uppercase opacity-80">
          STATUS: <span className="text-[#39ff14] animate-pulse">ONLINE</span> // OVERVIEW
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
        {stats.map((stat, i) => {
          const Icon = stat.icon;
          const offsetClass = i % 2 === 0 ? "mt-0 md:mt-4" : "mt-0 md:-mt-4";
          
          return (
            <div key={i} className={`retro-card ${offsetClass}`}>
              <div className="absolute top-0 right-0 p-2 bg-[#060b19] border-b-[3px] border-l-[3px]" style={{ borderColor: stat.color }}>
                <Icon size={24} style={{ color: stat.color }} />
              </div>
              
              <div className="mt-8">
                <p className="text-xs font-bold uppercase tracking-widest mb-2 opacity-80" style={{ color: stat.color }}>
                  &gt; {stat.name}
                </p>
                <p className="text-4xl font-bold text-white break-words">
                  {stat.value}
                </p>
              </div>
            </div>
          );
        })}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mt-12">
        <div className="retro-card">
          <div className="bg-[#ffea00] text-[#03040c] p-2 inline-block mb-6 font-bold uppercase shadow-[4px_4px_0px_0px_#ff00ff]">
            [WARNING: LOW STOCK ITEMS]
          </div>
          <div className="space-y-4">
            {products.filter(p => p.stockQuantity < 10).map(p => (
              <div key={p.id} className="flex flex-col sm:flex-row sm:items-center justify-between p-4 border-[2px] border-[#ffea00] bg-[#ffea00]/5 hover:bg-[#ffea00]/20 transition-none">
                <div className="flex items-center gap-3 mb-2 sm:mb-0">
                  <AlertTriangle size={18} className="text-[#ffea00]" />
                  <div>
                    <p className="font-bold text-white uppercase">{p.name}</p>
                    <p className="text-xs text-[#00ffcc]">{p.sku}</p>
                  </div>
                </div>
                <div className="text-left sm:text-right">
                  <p className="text-[#ffea00] font-bold text-xl">{p.stockQuantity} REMAINING</p>
                </div>
              </div>
            ))}
            {lowStock === 0 && (
              <p className="text-[#39ff14] p-4 border-[2px] border-[#39ff14] bg-[#39ff14]/10 uppercase font-bold text-center">
                ALL STOCK LEVELS HEALTHY
              </p>
            )}
          </div>
        </div>

        <div className="retro-card" style={{ boxShadow: '8px 8px 0px 0px #00ffcc' }}>
          <div className="bg-[#00ffcc] text-[#03040c] p-2 inline-block mb-6 font-bold uppercase shadow-[4px_4px_0px_0px_#ff00ff]">
            [RECENT ORDERS]
          </div>
          <div className="space-y-4">
            {orders.slice(0, 5).map(o => (
              <div key={o.id} className="flex flex-col sm:flex-row sm:items-center justify-between p-4 border-[2px] border-[#00ffcc] bg-[#00ffcc]/5 hover:bg-[#00ffcc]/20 transition-none">
                <div>
                  <p className="font-bold text-white uppercase">ORDER #{o.id}</p>
                  <p className="text-xs text-[#ff00ff]">{o.items?.length || 0} ITEMS</p>
                </div>
                <div className="text-left sm:text-right mt-2 sm:mt-0">
                  <p className="text-[#39ff14] font-bold text-xl">₹{o.totalAmount?.toLocaleString('en-IN', {minimumFractionDigits: 2}) || '0.00'}</p>
                  <span className="text-xs px-2 py-1 bg-[#ff00ff] text-[#03040c] font-bold uppercase inline-block mt-1">
                    {o.status}
                  </span>
                </div>
              </div>
            ))}
            {orders.length === 0 && (
              <p className="text-[#00ffcc] p-4 border-[2px] border-[#00ffcc] bg-[#00ffcc]/10 uppercase font-bold text-center">
                NO ORDERS YET
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
