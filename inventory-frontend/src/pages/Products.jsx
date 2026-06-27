import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';

const Products = () => {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showForm, setShowForm] = useState(false);
    const [editingProduct, setEditingProduct] = useState(null);
    const { isAdmin } = useAuth();

    const [formData, setFormData] = useState({
        sku: '',
        name: '',
        description: '',
        price: '',
        stock: '',
        category: ''
    });

    const fetchProducts = async () => {
        try {
            const response = await api.get('/products');
            setProducts(response.data);
        } catch (error) {
            console.error('Failed to fetch products', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchProducts();
    }, []);

    const handleInputChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            if (editingProduct) {
                await api.put(`/products/${editingProduct.id}`, formData);
            } else {
                await api.post('/products', formData);
            }
            setShowForm(false);
            setEditingProduct(null);
            setFormData({ sku: '', name: '', description: '', price: '', stock: '', category: '' });
            fetchProducts();
        } catch (error) {
            console.error('Failed to save product', error);
            alert('Failed to save product. Check console for details.');
        }
    };

    const handleDelete = async (id) => {
        if (window.confirm('Are you sure you want to delete this product?')) {
            try {
                await api.delete(`/products/${id}`);
                fetchProducts();
            } catch (error) {
                console.error('Failed to delete product', error);
            }
        }
    };

    const handleEdit = (product) => {
        setEditingProduct(product);
        setFormData({
            sku: product.sku,
            name: product.name,
            description: product.description || '',
            price: product.price,
            stock: product.stock,
            category: product.category || ''
        });
        setShowForm(true);
    };

    if (loading) return <div className="text-center py-10">Loading products...</div>;

    return (
        <div className="container mx-auto px-4 py-8">
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-2xl font-bold">📦 Products</h1>
                {isAdmin() && (
                    <button
                        onClick={() => { setShowForm(!showForm); setEditingProduct(null); if (!showForm) setFormData({ sku: '', name: '', description: '', price: '', stock: '', category: '' }); }}
                        className="bg-indigo-600 text-white px-4 py-2 rounded hover:bg-indigo-700"
                    >
                        {showForm ? 'Cancel' : '+ Add Product'}
                    </button>
                )}
            </div>

            {showForm && isAdmin() && (
                <form onSubmit={handleSubmit} className="bg-white shadow-md rounded px-8 pt-6 pb-8 mb-6 border">
                    <h2 className="text-xl font-semibold mb-4">{editingProduct ? 'Edit Product' : 'New Product'}</h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <input type="text" name="sku" placeholder="SKU" value={formData.sku} onChange={handleInputChange} className="border p-2 rounded" required />
                        <input type="text" name="name" placeholder="Name" value={formData.name} onChange={handleInputChange} className="border p-2 rounded" required />
                        <input type="text" name="description" placeholder="Description" value={formData.description} onChange={handleInputChange} className="border p-2 rounded" />
                        <input type="number" name="price" placeholder="Price" value={formData.price} onChange={handleInputChange} className="border p-2 rounded" required step="0.01" />
                        <input type="number" name="stock" placeholder="Stock" value={formData.stock} onChange={handleInputChange} className="border p-2 rounded" required />
                        <input type="text" name="category" placeholder="Category" value={formData.category} onChange={handleInputChange} className="border p-2 rounded" />
                    </div>
                    <button type="submit" className="mt-4 bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700">
                        {editingProduct ? 'Update' : 'Create'} Product
                    </button>
                </form>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {products.map((product) => (
                    <div key={product.id} className="bg-white shadow-lg rounded-lg overflow-hidden border hover:shadow-xl transition">
                        <div className="p-6">
                            <h3 className="text-xl font-semibold">{product.name}</h3>
                            <p className="text-gray-600 text-sm mt-1">SKU: {product.sku}</p>
                            <p className="text-gray-700 mt-2">{product.description || 'No description'}</p>
                            <div className="mt-4 flex justify-between items-center">
                                <span className="text-2xl font-bold text-indigo-600">${product.price}</span>
                                <span className="text-sm text-gray-600">Stock: {product.stock}</span>
                            </div>
                            {product.category && <span className="inline-block bg-gray-200 rounded-full px-3 py-1 text-sm font-semibold text-gray-700 mt-2">{product.category}</span>}

                            {isAdmin() && (
                                <div className="mt-4 flex gap-2">
                                    <button onClick={() => handleEdit(product)} className="bg-blue-500 text-white px-3 py-1 rounded hover:bg-blue-600 text-sm">Edit</button>
                                    <button onClick={() => handleDelete(product.id)} className="bg-red-500 text-white px-3 py-1 rounded hover:bg-red-600 text-sm">Delete</button>
                                </div>
                            )}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default Products;