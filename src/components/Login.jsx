import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { loginUser, clearError } from '../store/slices/authSlice';
import { useNavigate, Link, useLocation } from 'react-router-dom';

const Login = () => {
    const [formData, setFormData] = useState({
        username: '',
        password: '',
    });
    const [formErrors, setFormErrors] = useState({});
    const [isSubmitted, setIsSubmitted] = useState(false);

    const dispatch = useDispatch();
    const navigate = useNavigate();
    const location = useLocation();
    const { isLoading, error, isAuthenticated } = useSelector((state) => state.auth);

    // Получаем сообщение из state навигации (после регистрации)
    const successMessage = location.state?.message;
    const registeredUsername = location.state?.username;

    useEffect(() => {
        if (isAuthenticated) {
            navigate('/');
        }
    }, [isAuthenticated, navigate]);

    // Автозаполнение username после регистрации
    useEffect(() => {
        if (registeredUsername) {
            setFormData(prev => ({
                ...prev,
                username: registeredUsername
            }));
        }
    }, [registeredUsername]);

    const validateForm = () => {
        const errors = {};

        if (!formData.username.trim()) {
            errors.username = 'Username is required';
        } else if (!/^\w+$/.test(formData.username)) {
            errors.username = 'Username can only contain letters, numbers and underscores';
        }

        if (!formData.password) {
            errors.password = 'Password is required';
        } else if (formData.password.length < 6) {
            errors.password = 'Password must be at least 6 characters long';
        }

        return errors;
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));

        if (formErrors[name]) {
            setFormErrors(prev => ({
                ...prev,
                [name]: ''
            }));
        }

        if (error) {
            dispatch(clearError());
        }
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        setIsSubmitted(true);

        const errors = validateForm();
        setFormErrors(errors);

        if (Object.keys(errors).length === 0) {
            dispatch(loginUser(formData));
        }
    };

    const handleDemoLogin = () => {
        setFormData({
            username: 'demo_user',
            password: 'demo123'
        });
        // Очищаем ошибки при установке демо данных
        setFormErrors({});
        dispatch(clearError());
    };

    const getInputClassName = (fieldName) => {
        return `w-full px-4 py-3 border rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-colors ${
            formErrors[fieldName]
                ? 'border-red-300 bg-red-50'
                : 'border-gray-300 hover:border-gray-400'
        }`;
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 py-12 px-4 sm:px-6 lg:px-8">
            <div className="w-full max-w-md">
                {/* Card */}
                <div className="bg-white rounded-2xl shadow-xl p-8 mx-4">
                    {/* Header */}
                    <div className="text-center mb-8">
                        <div className="mx-auto w-16 h-16 bg-gradient-to-r from-indigo-500 to-purple-600 rounded-2xl flex items-center justify-center mb-4">
                            <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                            </svg>
                        </div>
                        <h2 className="text-3xl font-bold text-gray-900 mb-2">
                            Welcome Back
                        </h2>
                        <p className="text-gray-600">
                            Sign in to your account to continue
                        </p>
                    </div>

                    <form className="space-y-6" onSubmit={handleSubmit} noValidate>
                        {/* Success Message (after registration) */}
                        {successMessage && (
                            <div className="bg-green-50 border border-green-200 rounded-xl p-4">
                                <div className="flex items-center">
                                    <svg className="w-5 h-5 text-green-500 mr-2" fill="currentColor" viewBox="0 0 20 20">
                                        <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                                    </svg>
                                    <span className="text-green-700 font-medium text-sm">
                                        {successMessage}
                                    </span>
                                </div>
                            </div>
                        )}

                        {/* Server Error */}
                        {error && (
                            <div className="bg-red-50 border border-red-200 rounded-xl p-4">
                                <div className="flex items-center">
                                    <svg className="w-5 h-5 text-red-500 mr-2" fill="currentColor" viewBox="0 0 20 20">
                                        <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                                    </svg>
                                    <span className="text-red-700 font-medium text-sm">
                                        {typeof error === 'string' ? error : 'Authentication failed'}
                                    </span>
                                </div>
                            </div>
                        )}

                        <div className="space-y-5">
                            {/* Username Field */}
                            <div>
                                <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-2">
                                    Username
                                </label>
                                <input
                                    id="username"
                                    name="username"
                                    type="text"
                                    required
                                    className={getInputClassName('username')}
                                    placeholder="Enter your username"
                                    value={formData.username}
                                    onChange={handleChange}
                                    disabled={isLoading}
                                />
                                {formErrors.username && (
                                    <p className="mt-2 text-sm text-red-600 flex items-center">
                                        <svg className="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                                            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
                                        </svg>
                                        {formErrors.username}
                                    </p>
                                )}
                            </div>

                            {/* Password Field */}
                            <div>
                                <div className="flex items-center justify-between mb-2">
                                    <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                                        Password
                                    </label>
                                    <Link
                                        to="/forgot-password"
                                        className="text-sm text-indigo-600 hover:text-indigo-500 font-medium transition-colors"
                                    >
                                        Forgot password?
                                    </Link>
                                </div>
                                <input
                                    id="password"
                                    name="password"
                                    type="password"
                                    required
                                    className={getInputClassName('password')}
                                    placeholder="Enter your password"
                                    value={formData.password}
                                    onChange={handleChange}
                                    disabled={isLoading}
                                />
                                {formErrors.password && (
                                    <p className="mt-2 text-sm text-red-600 flex items-center">
                                        <svg className="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                                            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
                                        </svg>
                                        {formErrors.password}
                                    </p>
                                )}
                            </div>
                        </div>

                        {/* Demo Login Button */}
                        <button
                            type="button"
                            onClick={handleDemoLogin}
                            disabled={isLoading}
                            className="w-full bg-gradient-to-r from-gray-600 to-gray-700 text-white py-3 px-4 rounded-xl hover:from-gray-700 hover:to-gray-800 focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 font-medium shadow-lg hover:shadow-xl transform hover:-translate-y-0.5"
                        >
                            Try Demo Account
                        </button>

                        {/* Submit Button */}
                        <button
                            type="submit"
                            disabled={isLoading}
                            className="w-full bg-gradient-to-r from-indigo-600 to-purple-600 text-white py-3 px-4 rounded-xl hover:from-indigo-700 hover:to-purple-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 font-medium shadow-lg hover:shadow-xl transform hover:-translate-y-0.5"
                        >
                            {isLoading ? (
                                <div className="flex items-center justify-center space-x-2">
                                    <svg className="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    <span>Signing in...</span>
                                </div>
                            ) : (
                                'Sign in to your account'
                            )}
                        </button>

                        {/* Divider */}
                        <div className="relative">
                            <div className="absolute inset-0 flex items-center">
                                <div className="w-full border-t border-gray-300"></div>
                            </div>
                            <div className="relative flex justify-center text-sm">
                                <span className="px-2 bg-white text-gray-500">New to our platform?</span>
                            </div>
                        </div>

                        {/* Register Link */}
                        <div className="text-center">
                            <Link
                                to="/register"
                                className="inline-flex items-center text-indigo-600 hover:text-indigo-500 font-medium transition-colors group"
                            >
                                Create an account
                                <svg className="w-4 h-4 ml-1 transform group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                                </svg>
                            </Link>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default Login;