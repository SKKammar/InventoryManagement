import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { LayoutDashboard, Package, ShoppingCart, LogOut, Terminal } from 'lucide-react';
import { cn } from '../lib/utils';

export default function Navbar() {
  const { user, logout } = useAuth();
  const location = useLocation();

  const links = [
    { name: 'DASHBOARD', path: '/', icon: LayoutDashboard },
    { name: 'PRODUCTS', path: '/products', icon: Package },
    { name: 'ORDERS', path: '/orders', icon: ShoppingCart },
  ];

  return (
    <nav className="border-b-[4px] border-[#00ffcc] bg-[#060b19] px-6 py-4 flex flex-col md:flex-row items-start md:items-center justify-between gap-4 sticky top-0 z-50">
      <div className="flex items-center gap-6 w-full md:w-auto">
        <div className="flex items-center gap-2 border-[3px] border-[#ff00ff] p-2 bg-[#ff00ff]/10">
          <Terminal size={24} className="text-[#ffea00]" />
          <h1 className="text-xl font-bold text-[#ffea00] tracking-widest uppercase">
            GreatOne
          </h1>
        </div>
        <div className="flex items-center gap-4 flex-1">
          {links.map((link) => {
            const isActive = location.pathname === link.path;
            const Icon = link.icon;
            return (
              <Link
                key={link.name}
                to={link.path}
                className={cn(
                  "flex items-center gap-2 px-3 py-2 border-[2px] transition-none font-bold uppercase",
                  isActive 
                    ? "bg-[#00ffcc] text-[#03040c] border-[#00ffcc] shadow-[4px_4px_0px_0px_#ff00ff]" 
                    : "border-[#00ffcc] text-[#00ffcc] hover:bg-[#ff00ff] hover:text-[#03040c] hover:border-[#ff00ff] hover:shadow-[4px_4px_0px_0px_#ffea00]"
                )}
              >
                <Icon size={16} />
                {link.name}
              </Link>
            );
          })}
        </div>
      </div>
      
      <div className="flex items-center gap-4 border-[2px] border-[#ffea00] p-2 bg-[#ffea00]/10">
        <div className="flex items-center gap-3 text-sm">
          <span className="w-2 h-2 bg-[#39ff14] animate-pulse"></span>
          <span className="font-bold text-[#ffea00]">{user?.username || 'GUEST'}</span>
          <span className={cn(
            "text-[10px] font-bold px-2 py-0.5 uppercase tracking-wider",
            user?.role === 'ADMIN' 
              ? "bg-[#ff00ff] text-[#03040c]" 
              : "bg-[#00ffcc]/20 text-[#00ffcc] border border-[#00ffcc]"
          )}>
            {user?.role || 'UNKNOWN'}
          </span>
        </div>
        <button
          onClick={logout}
          className="text-[#ff00ff] hover:text-[#03040c] hover:bg-[#ff00ff] p-1 border border-[#ff00ff] transition-none flex items-center gap-1"
          title="Logout"
        >
          <LogOut size={16} />
          <span className="text-xs font-bold uppercase">EXIT</span>
        </button>
      </div>
    </nav>
  );
}
