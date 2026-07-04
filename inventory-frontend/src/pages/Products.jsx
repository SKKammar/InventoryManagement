import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { productSchema } from '../schemas/product';
import { Plus, Trash2, Edit, X } from 'lucide-react';
import { toast } from 'sonner';
import { useAuth } from '../context/AuthContext';

export default function Products() {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);

  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    resolver: zodResolver(productSchema)
  });

  const { data: products = [], isLoading } = useQuery({
    queryKey: ['products'],
    queryFn: async () => {
      const res = await axios.get('/api/products');
      return res.data;
    }
  });

  const mutation = useMutation({
    mutationFn: async (data) => {
      if (editingId) {
        return axios.put(`/api/products/${editingId}`, data);
      }
      return axios.post('/api/products', data);
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['products']);
      toast.success(editingId ? 'Product updated successfully' : 'Product created successfully');
      closeModal();
    },
    onError: (err) => {
      const errorMsg = err.response?.data?.error || err.response?.data || 'An error occurred';
      toast.error(errorMsg);
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (id) => axios.delete(`/api/products/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries(['products']);
      toast.success('Product deleted successfully');
    },
    onError: (err) => {
      const errorMsg = err.response?.data?.error || 'Failed to delete product';
      toast.error(errorMsg);
    }
  });

  const openModal = (product = null) => {
    if (product) {
      setEditingId(product.id);
      reset(product);
    } else {
      setEditingId(null);
      reset({
        sku: '', name: '', description: '', price: '', stockQuantity: '', category: ''
      });
    }
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setEditingId(null);
    reset();
  };

  const onSubmit = (data) => {
    mutation.mutate(data);
  };

  if (isLoading) return (
    <div className="flex items-center gap-4 mt-10">
      <div className="w-4 h-4 bg-[#ffea00] animate-ping"></div>
      <div className="text-[#ffea00] font-bold text-xl uppercase tracking-widest">LOADING DATA...</div>
    </div>
  );

  return (
    <div className="space-y-8 crt-effect">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-6 border-b-[4px] border-[#ff00ff] pb-4">
        <div>
          <h2 className="text-4xl font-bold text-[#39ff14] tracking-widest uppercase">
            PRODUCT RECORDS
          </h2>
          <p className="text-[#ff00ff] mt-2 font-mono uppercase opacity-80 font-bold">
            &gt; Manage your product inventory
          </p>
        </div>
        {isAdmin && (
          <button
            onClick={() => openModal()}
            className="retro-btn bg-[#ff00ff] text-[#03040c] border-[#ff00ff] hover:bg-[#39ff14] hover:text-[#03040c] hover:border-[#39ff14] flex items-center justify-center gap-2"
            style={{ boxShadow: '4px 4px 0px 0px #39ff14' }}
          >
            <Plus size={18} />
            ADD PRODUCT
          </button>
        )}
      </div>

      <div className="retro-table-container">
        <table className="w-full retro-table">
          <thead className="bg-[#03040c]">
            <tr>
              <th>SKU</th>
              <th>NAME</th>
              <th>CATEGORY</th>
              <th>PRICE</th>
              <th>QUANTITY</th>
              {isAdmin && <th className="text-right">ACTIONS</th>}
            </tr>
          </thead>
          <tbody>
            {products.map((product) => (
              <tr key={product.id}>
                <td className="text-[#00ffcc]">{product.sku}</td>
                <td className="font-bold text-white uppercase">{product.name}</td>
                <td>
                  <span className="bg-[#ff00ff] text-[#03040c] px-2 py-1 font-bold text-xs uppercase">
                    {product.category || 'N/A'}
                  </span>
                </td>
                <td className="text-[#39ff14] font-bold">₹{product.price?.toLocaleString('en-IN', {minimumFractionDigits: 2})}</td>
                <td>
                  <div className="flex items-center gap-2">
                    <div className={`w-3 h-3 border border-black ${product.stockQuantity > 10 ? 'bg-[#39ff14]' : product.stockQuantity === 0 ? 'bg-[#ff0000] animate-pulse' : 'bg-[#ffea00] animate-pulse'}`} />
                    <span className="font-bold">{product.stockQuantity}</span>
                    {product.stockQuantity === 0 && <span className="text-[#ff0000] text-xs font-bold">OUT OF STOCK</span>}
                  </div>
                </td>
                {isAdmin && (
                  <td className="text-right">
                    <div className="flex justify-end gap-2">
                      <button 
                        onClick={() => openModal(product)} 
                        className="bg-[#03040c] text-[#00ffcc] border-[2px] border-[#00ffcc] p-2 hover:bg-[#00ffcc] hover:text-[#03040c]"
                      >
                        <Edit size={16} />
                      </button>
                      <button 
                        onClick={() => {
                          if (confirm('Are you sure you want to delete this product?')) {
                            deleteMutation.mutate(product.id);
                          }
                        }}
                        className="bg-[#03040c] text-[#ff00ff] border-[2px] border-[#ff00ff] p-2 hover:bg-[#ff00ff] hover:text-[#03040c]"
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </td>
                )}
              </tr>
            ))}
            {products.length === 0 && (
              <tr>
                <td colSpan={isAdmin ? "6" : "5"} className="p-8 text-center text-[#ffea00] font-bold uppercase">
                  NO PRODUCTS FOUND
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {isModalOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-[#03040c]/80 backdrop-blur-sm" onClick={closeModal} />
          
          <div className="retro-card w-full max-w-2xl bg-[#060b19] border-[#ffea00] shadow-[8px_8px_0px_0px_#ff00ff] relative z-10">
            <div className="flex justify-between items-center border-b-[3px] border-[#ffea00] pb-4 mb-6">
              <h3 className="text-2xl font-bold text-[#ffea00] uppercase tracking-widest">
                {editingId ? 'EDIT PRODUCT' : 'NEW PRODUCT'}
              </h3>
              <button onClick={closeModal} className="text-[#ff00ff] hover:text-white bg-[#03040c] border-[2px] border-[#ff00ff] p-1">
                <X size={20} />
              </button>
            </div>
            
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                <div className="space-y-2">
                  <label className="text-xs font-bold text-[#00ffcc] uppercase tracking-wider block">
                    &gt; SKU CODE
                  </label>
                  <input {...register('sku')} className="retro-input" placeholder="e.g. ITEM-001" />
                  {errors.sku && <p className="text-[#ff00ff] font-bold text-xs bg-[#ff00ff]/10 p-1 border border-[#ff00ff] mt-1">{errors.sku.message}</p>}
                </div>
                <div className="space-y-2">
                  <label className="text-xs font-bold text-[#00ffcc] uppercase tracking-wider block">
                    &gt; CATEGORY
                  </label>
                  <input {...register('category')} className="retro-input" placeholder="e.g. Hardware" />
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-xs font-bold text-[#00ffcc] uppercase tracking-wider block">
                  &gt; PRODUCT NAME
                </label>
                <input {...register('name')} className="retro-input" />
                {errors.name && <p className="text-[#ff00ff] font-bold text-xs bg-[#ff00ff]/10 p-1 border border-[#ff00ff] mt-1">{errors.name.message}</p>}
              </div>

              <div className="space-y-2">
                <label className="text-xs font-bold text-[#00ffcc] uppercase tracking-wider block">
                  &gt; DESCRIPTION
                </label>
                <textarea {...register('description')} rows="3" className="retro-input resize-none" />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                <div className="space-y-2">
                  <label className="text-xs font-bold text-[#00ffcc] uppercase tracking-wider block">
                    &gt; PRICE (₹)
                  </label>
                  <input type="number" step="0.01" {...register('price')} className="retro-input" />
                  {errors.price && <p className="text-[#ff00ff] font-bold text-xs bg-[#ff00ff]/10 p-1 border border-[#ff00ff] mt-1">{errors.price.message}</p>}
                </div>
                <div className="space-y-2">
                  <label className="text-xs font-bold text-[#00ffcc] uppercase tracking-wider block">
                    &gt; STOCK QUANTITY
                  </label>
                  <input type="number" {...register('stockQuantity')} className="retro-input" />
                  {errors.stockQuantity && <p className="text-[#ff00ff] font-bold text-xs bg-[#ff00ff]/10 p-1 border border-[#ff00ff] mt-1">{errors.stockQuantity.message}</p>}
                </div>
              </div>

              <div className="flex flex-col sm:flex-row justify-end gap-4 mt-8 border-t-[3px] border-[#ffea00] pt-6">
                <button type="button" onClick={closeModal} className="retro-btn-danger">
                  CANCEL
                </button>
                <button type="submit" disabled={mutation.isPending} className="retro-btn" style={{ borderColor: '#39ff14', color: '#39ff14', boxShadow: '4px 4px 0px 0px #39ff14'}}>
                  {mutation.isPending ? 'SAVING...' : 'SAVE PRODUCT'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
