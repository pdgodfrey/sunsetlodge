import { useAuthStore } from '@/stores/auth';

export const fetchWrapper = {
    get: request('GET'),
    post: request('POST'),
    postForm: formDataRequest('POST'),
    put: request('PUT'),
    delete: request('DELETE')
};

function request(method: string) {
    return (url: any, body?: any) => {
        const requestOptions: any = {
            method,
            headers: authHeader(url),
            credentials: 'include'
        };
        if (body) {
            requestOptions.headers['Content-Type'] = 'application/json';
            requestOptions.body = JSON.stringify(body);
        }

        return fetch(url, requestOptions).then(handleResponse);
    };
}

function formDataRequest(method: String) {
  return (url: any, formData?: FormData) => {
    const requestOptions: any = {
      method,
      headers: authHeader(url),
      credentials: 'include'
    };

    if(formData){
      // requestOptions.headers['Content-Type'] = "application/x-www-form-urlencoded";
      requestOptions.body = formData;
    }

    return fetch(url, requestOptions).then(handleResponse);
  };
}

// helper functions

function authHeader(url: any) {
    // return auth header with jwt if user is logged in and request is to the api url
    const { authToken } = useAuthStore();
    // console.log(`authToken: ${authToken}`)
    const isLoggedIn = (authToken != '');
    // console.log(`isLoggedIn: ${isLoggedIn}`)
    const isApiUrl = url.startsWith(import.meta.env.VITE_API_URL);
    // console.log(`isApiUrl: ${isApiUrl}`)
    if (isLoggedIn && (isApiUrl || import.meta.env.PROD)) {
        return { Authorization: `Bearer ${authToken}` };
    } else {
        return {};
    }
}

function handleResponse(response: any) {
    // return response.text().then((text: any) => {
    //     const data = text && JSON.parse(text);
    //
    //     if (!response.ok) {
    //         const { user, logout } = useAuthStore();
    //         if ([401, 403].includes(response.status) && user) {
    //             // auto logout if 401 Unauthorized or 403 Forbidden response returned from api
    //             logout();
    //         }
    //
    //         const error = (data && data.message) || response.statusText;
    //         return Promise.reject(error);
    //     }
    //
    //     return data;
    // });

    return response.text().then((text: any) => {
        if (!response.ok) {
            const { user, logout } = useAuthStore();
            if ([401, 403].includes(response.status) && user) {
                if(response.url.indexOf("api/auth/logout") == -1){

                    // auto logout if 401 Unauthorized or 403 Forbidden response returned from api
                    logout();
                }
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
