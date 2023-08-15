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
        if (body && body != {}) {
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
    const isLoggedIn = (authToken != '');
    const isApiUrl = url.startsWith(import.meta.env.VITE_API_URL);
    if (isLoggedIn && isApiUrl) {
        return { Authorization: `Bearer ${authToken}` };
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
