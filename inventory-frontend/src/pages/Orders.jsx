import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';

const Orders = () => {
    const [orders, setOrders] = useState([]);
    const [products, setProducts] = useState([]);
    const [selectedProduct, setSelectedProduct] = useState('');
    const [quantity, setQuantity] = useState(1);
    const [loading, setLoading] = useState(true);
    const { user } = useAuth();

    const fetchOrders = async () => {
        try {
            const response = await api.get('/orders');
            setOrders(response.data);
        } catch (error) {
            console.error('Failed to fetch orders', error);
        }
    };

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
        fetchOrders();
        fetchProducts();
    }, []);

    const handlePlaceOrder = async (e) => {
        e.preventDefault();
        if (!selectedProduct || quantity < 1) return;

        try {
            await api.post('/orders', [{ productId: parseInt(selectedProduct), quantity: parseInt(quantity) }]);
            alert('✅ Order placed successfully!');
            setSelectedProduct('');
            setQuantity(1);
            fetchOrders();
        } catch (error) {
            console.error('Failed to place order', error);
            alert('❌ Failed to place order. Check stock or login status.');
        }
    };

    if (loading) return <div className="text-center py-10">Loading...</div>;

    return (
        <div className="container mx-auto px-4 py-8">
            <h1 className="text-2xl font-bold mb-6">📋 My Orders</h1>

            {user && (
                <div className="bg-white shadow-md rounded px-8 pt-6 pb-8 mb-8 border">
                    <h2 className="text-xl font-semibold mb-4">🛒 Place New Order</h2>
                    <form onSubmit={handlePlaceOrder} className="flex gap-4 items-end flex-wrap">
                        <div className="flex-1 min-w-[200px]">
                            <label className="block text-gray-700 text-sm font-bold mb-2">Product</label>
                            <select
                                value={selectedProduct}
                                onChange={(e) => setSelectedProduct(e.target.value)}
                                className="border p-2 rounded w-full"
                                required
                            >
                                <option value="">Select a product...</option>
                                {products.map(p => (
                                    <option key={p.id} value={p.id}>{p.name} (${p.price} - Stock: {p.stock})</option>
                                ))}
                            </select>
                        </div>
                        <div className="w-32">
                            <label className="block text-gray-700 text-sm font-bold mb-2">Quantity</label>
                            <input
                                type="number"
                                min="1"
                                value={quantity}
                                onChange={(e) => setQuantity(e.target.value)}
                                className="border p-2 rounded w-full"
                                required
                            />
                        </div>
                        <button type="submit" className="bg-indigo-600 text-white px-6 py-2 rounded hover:bg-indigo-700">
                            Place Order
                        </button>
                    </form>
                </div>
            )}

            <div className="space-y-4">
                {orders.length === 0 ? (
                    <p className="text-gray-500">No orders yet. Place your first order above!</p>
                ) : (
                    orders.map((order) => (
                        <div key={order.id} className="bg-white shadow-md rounded-lg p-6 border">
                            <div className="flex justify-between items-center">
                                <div>
                                    <span className="font-bold">Order #{order.id}</span>
                                    <span className={`ml-4 px-2 py-1 rounded text-sm ${
                                        order.status === 'DELIVERED' ? 'bg-green-200 text-green-800' :
                                            order.status === 'CANCELLED' ? 'bg-red-200 text-red-800' :
                                                'bg-yellow-200 text-yellow-800'
                                    }`}>Status: {order.status}</span>
                                </div>
                                <span className="text-xl font-bold text-indigo-600">${order.totalAmount}</span>
                            </div>
                            <div className="mt-4 text-sm text-gray-600">
                                Placed on: {new Date(order.createdAt).toLocaleString()}
                            </div>
                            <div className="mt-2">
                                {order.items && order.items.map((item, idx) => (
                                    <div key={idx} className="border-t py-2 flex justify-between">
                                        <span>Product ID: {item.productId}</span>
                                        <span>Qty: {item.quantity} x ${item.price}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
};

export default Orders;