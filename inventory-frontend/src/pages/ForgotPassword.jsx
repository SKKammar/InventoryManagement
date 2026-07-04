import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import { toast } from 'sonner';
import { Terminal, Mail } from 'lucide-react';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const response = await axios.post('/api/auth/forgot-password', { email });
      toast.success(response.data || 'Reset link sent');
    } catch (err) {
      toast.error(err.response?.data || 'Request failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-[80vh] w-full flex items-center justify-center p-4 crt-effect">
      
      <div className="w-full max-w-md relative">
        <div className="absolute -top-4 -left-4 w-8 h-8 border-t-4 border-l-4 border-[#ff00ff]"></div>
        <div className="absolute -bottom-4 -right-4 w-8 h-8 border-b-4 border-r-4 border-[#ff00ff]"></div>
        
        <div className="retro-card p-8">
          <div className="flex flex-col items-center mb-8 border-b-[3px] border-[#00ffcc] pb-6">
            <div className="bg-[#ff00ff] text-[#03040c] p-3 shadow-[4px_4px_0px_0px_#00ffcc] mb-4">
              <Terminal size={40} />
            </div>
            <h2 className="text-3xl font-bold text-[#ffea00] mb-2 uppercase tracking-widest text-center">
              FORGOT PASSWORD
            </h2>
            <p className="text-[#00ffcc] text-center text-xs uppercase opacity-80">
              Enter your email to receive a reset link.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="space-y-2">
              <label className="text-xs font-bold text-[#39ff14] uppercase tracking-wider block">
                &gt; EMAIL ADDRESS
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Mail size={18} className="text-[#00ffcc]" />
                </div>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="retro-input pl-10"
                  placeholder="you@example.com"
                  required
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="retro-btn w-full mt-8"
            >
              {loading ? 'SENDING...' : 'SEND RESET LINK'}
            </button>
            
            <div className="text-center mt-4">
              <Link to="/login" className="text-[#00ffcc] hover:text-[#ffea00] text-sm uppercase tracking-wider">
                &lt; BACK TO LOGIN
              </Link>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
