import { useAuthStore } from '@/stores/auth';

export const fetchWrapper = {
    get: request('GET'),
    post: request('POST'),
    put: request('PUT'),
    delete: request('DELETE')
};

function request(method: string) {
    return (url: any, body?: any) => {
        const requestOptions: any = {
            method,
            headers: authHeader(url),
        };
        if (body) {
            requestOptions.headers['Content-Type'] = 'application/json';
            requestOptions.body = JSON.stringify(body);
        }

        requestOptions.credentials = 'include'

        return fetch(url, requestOptions).then(handleResponse);
    };
}

// helper functions

function authHeader(url: any) {
    // return auth header with jwt if user is logged in and request is to the api url
    const { user } = useAuthStore();
    const isLoggedIn = !!user?.token;
    const isApiUrl = url.startsWith(import.meta.env.VITE_API_URL);
    if (isLoggedIn && isApiUrl) {
        return { Authorization: `Bearer ${user.token}` };
    } else {
        return { };
    }
}

function handleResponse(response: any) {

    // if(!response.ok) {
    //   const { user, logout } = useAuthStore();
    //   if ([401, 403].includes(response.status) && user) {
    //     // auto logout if 401 Unauthorized or 403 Forbidden response returned from api
    //     logout();
    //   }
    //
    //   const error = response.text();
    //   return Promise.reject(error);
    // } else {
    //   return response
    // }


    return response.text().then((text: any) => {
        if (!response.ok) {
            const { user, logout } = useAuthStore();
            if ([401, 403].includes(response.status) && user) {
                // auto logout if 401 Unauthorized or 403 Forbidden response returned from api
                logout();
            }

            const error = text;
            return Promise.reject(error);
        } else {
          try {
            const data = text && JSON.parse(text);
            return data;
          } catch(e) {
            return text
          }
        }

    });
}
