import axios from 'axios';

const API_BASE_URL = 'http://agi-gateway.social-network-gateway:8080';

const api = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true,
});

export const authAPI = {
    login: (credentials) => api.post('/auth/login', credentials),
    register: (userData) => api.post('/auth/register', userData),
    validateToken: () => api.get('/auth/validate-token'),
    logout: () => api.post('/auth/logout'),
    getUserInfo: () => api.get('/auth/user-info'),
    refreshToken: (refreshToken) => api.post('/auth/refresh', { refreshToken }),
};

export const imageAPI = {
    getAllImages: (page = 0, size = 12) =>
        api.get(`/api/images?page=${page}&size=${size}`),

    getUserImages: (userId, page = 0, size = 12) =>
        api.get(`/api/images/user/${userId}?page=${page}&size=${size}`),

    uploadImage: (formData) => api.post('/api/images/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    }),

    deleteImage: (imageId) => api.delete(`/api/images/${imageId}`),

    getImageById: (imageId) => api.get(`/api/images/${imageId}`),
};

export const commentAPI = {
    getImageComments: (imageId) => api.get(`/api/comments/image/${imageId}`),
    createComment: (commentData) => api.post('/api/comments', commentData),
    deleteComment: (commentId) => api.delete(`/api/comments/${commentId}`),
    getCommentById: (commentId) => api.get(`/api/comments/${commentId}`),
};

export const likeAPI = {
    toggleLike: (imageId, userId) => api.post(`/api/likes/images/${imageId}/likes`, null, {
        headers: {
            'X-User-Id': userId
        }
    }),
    getImageLikes: (imageId) => api.get(`/api/likes/image/${imageId}/count`),
    checkLike: (userId, imageId) => api.get(`/api/likes/check?userId=${userId}&imageId=${imageId}`),
    getImageLikesList: (imageId) => api.get(`/api/likes/image/${imageId}`),
};

api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                const refreshToken = localStorage.getItem('refreshToken');
                if (refreshToken) {
                    const response = await authAPI.refreshToken(refreshToken);
                    const newToken = response.data.accessToken;

                    localStorage.setItem('token', newToken);

                    originalRequest.headers.Authorization = `Bearer ${newToken}`;
                    return api(originalRequest);
                }
            } catch (refreshError) {
                console.error('Refresh token failed:', refreshError);
                localStorage.removeItem('token');
                localStorage.removeItem('refreshToken');
                localStorage.removeItem('user');
                window.location.href = '/login';
                return Promise.reject(refreshError);
            }
        }

        if (error.response?.status === 401) {
            localStorage.removeItem('token');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');
            window.location.href = '/login';
        }

        return Promise.reject(error);
    }
);

export default api;