import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { authAPI } from '../../services/api';

// Async thunks
export const loginUser = createAsyncThunk(
    'auth/login',
    async (credentials, { rejectWithValue }) => {
        try {
            const response = await authAPI.login(credentials);
            const { token, accessToken, refreshToken, username, userId, message } = response.data;

            const actualToken = accessToken || token;

            return {
                token: actualToken,
                refreshToken: refreshToken,
                user: {
                    id: userId,
                    username: username,
                }
            };
        } catch (error) {
            return rejectWithValue(error.response?.data?.error || error.response?.data?.message || 'Login failed');
        }
    }
);

export const registerUser = createAsyncThunk(
    'auth/register',
    async (userData, { rejectWithValue }) => {
        try {
            const response = await authAPI.register(userData);
            const { token, accessToken, refreshToken, username, userId } = response.data;

            const actualToken = accessToken || token;
            return {
                token: actualToken,
                refreshToken: refreshToken,
                user: {
                    id: userId,
                    username: username,
                }
            };
        } catch (error) {
            return rejectWithValue(error.response?.data?.error || error.response?.data?.message || 'Registration failed');
        }
    }
);

export const validateToken = createAsyncThunk(
    'auth/validateToken',
    async (_, { rejectWithValue }) => {
        try {
            const response = await authAPI.validateToken();
            return response.data;
        } catch (error) {
            return rejectWithValue('Token validation failed');
        }
    }
);

export const logoutUser = createAsyncThunk(
    'auth/logout',
    async (_, { rejectWithValue }) => {
        try {
            await authAPI.logout();
            return true;
        } catch (error) {
            return rejectWithValue('Logout failed');
        }
    }
);

export const refreshToken = createAsyncThunk(
    'auth/refreshToken',
    async (_, { rejectWithValue }) => {
        try {
            const refreshToken = localStorage.getItem('refreshToken');
            if (!refreshToken) {
                throw new Error('No refresh token available');
            }

            const response = await authAPI.refreshToken(refreshToken);
            return response.data;
        } catch (error) {
            return rejectWithValue('Token refresh failed');
        }
    }
);

const authSlice = createSlice({
    name: 'auth',
    initialState: {
        user: JSON.parse(localStorage.getItem('user')) || null,
        token: localStorage.getItem('token') || null,
        refreshToken: localStorage.getItem('refreshToken') || null,
        isLoading: false,
        error: null,
        isAuthenticated: !!localStorage.getItem('token'),
        // Добавляем состояние для регистрации
        registrationSuccess: false,
        registeredUser: null
    },
    reducers: {
        logout: (state) => {
            state.user = null;
            state.token = null;
            state.refreshToken = null;
            state.isAuthenticated = false;
            state.registrationSuccess = false;
            state.registeredUser = null;
            localStorage.removeItem('token');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');
        },
        clearError: (state) => {
            state.error = null;
        },
        updateToken: (state, action) => {
            state.token = action.payload;
            localStorage.setItem('token', action.payload);
        },
        // Новый reducer для очистки состояния регистрации
        clearRegistration: (state) => {
            state.registrationSuccess = false;
            state.registeredUser = null;
        }
    },
    extraReducers: (builder) => {
        builder
            // Login
            .addCase(loginUser.pending, (state) => {
                state.isLoading = true;
                state.error = null;
            })
            .addCase(loginUser.fulfilled, (state, action) => {
                state.isLoading = false;
                state.isAuthenticated = true;
                state.token = action.payload.token;
                state.refreshToken = action.payload.refreshToken;
                state.user = action.payload.user;
                state.registrationSuccess = false; // Сбрасываем при логине

                localStorage.setItem('token', state.token);
                if (state.refreshToken) {
                    localStorage.setItem('refreshToken', state.refreshToken);
                }
                localStorage.setItem('user', JSON.stringify(state.user));
            })
            .addCase(loginUser.rejected, (state, action) => {
                state.isLoading = false;
                state.error = action.payload;
            })
            // Register
            .addCase(registerUser.pending, (state) => {
                state.isLoading = true;
                state.error = null;
                state.registrationSuccess = false;
            })
            .addCase(registerUser.fulfilled, (state, action) => {
                state.isLoading = false;
                state.registrationSuccess = true;
                state.registeredUser = {
                    username: action.payload.username,
                    userId: action.payload.userId
                };
            })
            .addCase(registerUser.rejected, (state, action) => {
                state.isLoading = false;
                state.error = action.payload;
                state.registrationSuccess = false;
            })
            // Logout
            .addCase(logoutUser.fulfilled, (state) => {
                state.user = null;
                state.token = null;
                state.refreshToken = null;
                state.isAuthenticated = false;
                state.registrationSuccess = false;
                state.registeredUser = null;
                localStorage.removeItem('token');
                localStorage.removeItem('refreshToken');
                localStorage.removeItem('user');
            })
            // Refresh Token
            .addCase(refreshToken.fulfilled, (state, action) => {
                const { accessToken, token } = action.payload;
                const newToken = accessToken || token;
                if (newToken) {
                    state.token = newToken;
                    localStorage.setItem('token', newToken);
                }
            })
            .addCase(refreshToken.rejected, (state) => {
                state.user = null;
                state.token = null;
                state.refreshToken = null;
                state.isAuthenticated = false;
                state.registrationSuccess = false;
                state.registeredUser = null;
                localStorage.removeItem('token');
                localStorage.removeItem('refreshToken');
                localStorage.removeItem('user');
            });
    },
});

export const { logout, clearError, updateToken, clearRegistration } = authSlice.actions;
export default authSlice.reducer;