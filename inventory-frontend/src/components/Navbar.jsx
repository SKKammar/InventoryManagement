import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const Navbar = () => {
    const { user, logout, isAdmin } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    return (
        <nav className="bg-gray-800 text-white p-4 shadow-lg">
            <div className="container mx-auto flex justify-between items-center">
                <Link to="/products" className="text-xl font-bold">📦 Inventory</Link>
                <div className="flex gap-4 items-center">
                    <Link to="/products" className="hover:text-gray-300">Products</Link>
                    {user && <Link to="/orders" className="hover:text-gray-300">My Orders</Link>}
                    {!user ? (
                        <>
                            <Link to="/login" className="hover:text-gray-300">Login</Link>
                            <Link to="/register" className="hover:text-gray-300">Register</Link>
                        </>
                    ) : (
                        <>
                            <span className="text-sm text-gray-300 bg-gray-700 px-2 py-1 rounded">({user.role})</span>
                            <button onClick={handleLogout} className="bg-red-600 px-3 py-1 rounded hover:bg-red-700">Logout</button>
                        </>
                    )}
                </div>
            </div>
        </nav>
    );
};

export default Navbar;