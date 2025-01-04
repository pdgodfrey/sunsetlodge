// project imports
import mock from './mockAdapter';

import type {
    notificationType,
    profileType,
    languageType,
    searchType,
    quickLinksType,
    appsLinkType,
    MessagesType,
    SideProfielType
} from '@/types/HeaderTypes';
//
// Notification
//
import user1 from '@/assets/images/profile/user-1.jpg';
import user2 from '@/assets/images/profile/user-2.jpg';
import user3 from '@/assets/images/profile/user-3.jpg';
import user4 from '@/assets/images/profile/user-4.jpg';
import user5 from '@/assets/images/profile/user-5.jpg';
import user6 from '@/assets/images/profile/user-6.jpg';

const Messages: MessagesType[] = [
    {
        avatar: user1,
        title: 'Launch Admin',
        subtitle: 'Just see the my new admin!',
        time: '9:30 AM'
    },
    {
        avatar: user2,
        title: 'Event Today',
        subtitle: 'Just a reminder that you have event',
        time: '9:15 AM'
    },
    {
        avatar: user3,
        title: 'Settings',
        subtitle: 'You can customize this template as you want',
        time: '4:36 PM'
    },
    {
        avatar: user4,
        title: 'Launch Admin',
        subtitle: 'Just see the my new admin!',
        time: '9:30 AM'
    },
    {
        avatar: user5,
        title: 'Event Today',
        subtitle: 'Just a reminder that you have event',
        time: '9:15 AM'
    },
    {
        avatar: user6,
        title: 'Settings',
        subtitle: 'You can customize this template as you want',
        time: '4:36 PM'
    }
];

const notifications: notificationType[] = [
    {
        avatar: 'widget-3-line-duotone',
        color: 'primary',
        title: 'Launch Admin',
        subtitle: 'Just see the my new admin!',
        time: '9:30 AM'
    },
    {
        avatar: 'calendar-line-duotone',
        color: 'secondary',
        title: 'Event Today',
        subtitle: 'Just a reminder that you have event',
        time: '9:15 AM'
    },
    {
        avatar: 'settings-line-duotone',
        color: 'error',
        title: 'Settings',
        subtitle: 'You can customize this template as you want',
        time: '4:36 PM'
    },
    {
        avatar: 'widget-4-line-duotone',
        color: 'warning',
        title: 'Launch Admin',
        subtitle: 'Just see the my new admin!',
        time: '9:30 AM'
    },
    {
        avatar: 'calendar-line-duotone',
        color: 'success',
        title: 'Event Today',
        subtitle: 'Just a reminder that you have event',
        time: '9:15 AM'
    },
    {
        avatar: 'settings-line-duotone',
        color: 'info',
        title: 'Settings',
        subtitle: 'You can customize this template as you want',
        time: '4:36 PM'
    }
];

// Profile
//
const profileDD: profileType[] = [
    // {
    //     title: 'My Profile',
    //     href: '/apps/user/profile',
    //     badge: false
    // },
    // {
    //     title: 'My Subscription',
    //     href: '/pages/pricing',
    //     badge: false
    // },
    // {
    //     title: 'My Notes',
    //     href: '/apps/notes',
    //     badge: true
    // },
    // {
    //     title: 'Account Settings',
    //     href: '/pages/account-settings',
    //     badge: false
    // },
    {
        title: 'Sign Out',
        href: '/auth/logout',
        badge: false
    }
];

//
// Language
//
import flag1 from '@/assets/images/flag/icon-flag-en.svg';
import flag2 from '@/assets/images/flag/icon-flag-fr.svg';
import flag3 from '@/assets/images/flag/icon-flag-ro.svg';
import flag4 from '@/assets/images/flag/icon-flag-zh.svg';

const languageDD: languageType[] = [
    { title: 'English', subtext: 'UK', value: 'en', avatar: flag1 },
    { title: 'français', subtext: 'French', value: 'fr', avatar: flag2 },
    { title: 'عربي', subtext: 'Arbic', value: 'ro', avatar: flag3 },
    { title: '中国人', subtext: 'Chinese', value: 'zh', avatar: flag4 }
];

//
// AppsLink
//
const appsLink: appsLinkType[] = [
    {
        avatar: 'chat-line-linear',
        color: 'primary',
        title: 'Chat Application',
        subtext: 'New messages arrived',
        href: '/apps/chats'
    },
    {
        avatar: 'user-linear',
        color: 'success',
        title: 'User Profile App',
        subtext: 'Get profile details',
        href: '/apps/user/profile'
    },
    {
        avatar: 'bill-list-linear',
        color: 'secondary',
        title: 'eCommerce App',
        subtext: 'learn more information',
        href: '/ecommerce/products'
    },

    {
        avatar: 'calendar-minimalistic-linear',
        color: 'primary',
        title: 'Calendar App',
        subtext: 'Get dates',
        href: '/apps/calendar'
    },
    {
        avatar: 'phone-calling-rounded-linear',
        color: 'warning',
        title: 'Contact Application',
        subtext: '2 Unsaved Contacts',
        href: '/apps/contacts'
    },
    {
        avatar: 'bill-check-broken',
        color: 'success',
        title: 'Invoice App',
        subtext: 'Get Latest Invoice',
        href: '/apps/invoice'
    },
    {
        avatar: 'checklist-minimalistic-linear',
        color: 'error',
        title: 'Kanban App',
        subtext: 'Get new Task',
        href: '/apps/kanban'
    },
    {
        avatar: 'notes-linear',
        color: 'warning',
        title: 'Notes Application',
        subtext: 'To-do and Daily tasks',
        href: '/apps/notes'
    }
];

// Side Profile
const SideProfile: SideProfielType[] = [
    {
        icon: 'user-circle-outline',
        color: 'primary',
        title: 'My Profile',
        href: '/apps/user/profile'
    },
    {
        icon: 'notification-unread-lines-outline',
        color: 'secondary',
        title: 'My Notes',
        href: '/apps/notes'
    },
    {
        icon: 'inbox-line-outline',
        color: 'success',
        title: 'Inbox',
        href: '/apps/email'
    },
    {
        icon: 'settings-linear',
        color: 'warning',
        title: 'Account Settings',
        href: '/pages/account-settings'
    },
    {
        icon: 'login-2-outline',
        color: 'error',
        title: 'Logout',
        href: '/auth/login'
    },
]

//
// Quick Links
//
const quickLink: quickLinksType[] = [
    {
        title: 'Pricing Page',
        href: '/pages/pricing'
    },
    {
        title: 'Authentication Design',
        href: '/auth/login'
    },
    {
        title: '404 Error Page',
        href: '/auth/404'
    },
    {
        title: 'Notes App',
        href: '/apps/notes'
    },
    {
        title: 'User Application',
        href: '/apps/user/profile'
    },
    {
        title: 'Blog Design',
        href: '/apps/blog/posts'
    },
    {
        title: 'Shopping Cart',
        href: '/ecommerce/checkout'
    }
];

//
// Search Data
//
const searchSugg: searchType[] = [
    {
        title: 'Modern',
        href: '/dashboards/modern'
    },
    {
        title: 'eCommerce',
        href: '/dashboards/ecommerce'
    },
    {
        title: 'Contacts',
        href: '/apps/contacts'
    },
    {
        title: 'Shop',
        href: '/ecommerce/shop'
    },
    {
        title: 'Checkout',
        href: '/ecommerce/checkout'
    },
    {
        title: 'Chats',
        href: '/apps/chats'
    },
    {
        title: 'Notes',
        href: '/apps/notes'
    },
    {
        title: 'Pricing',
        href: '/pages/pricing'
    },
    {
        title: 'Account Setting',
        href: '/pages/account-settings'
    }
];

export { notifications, profileDD, languageDD, searchSugg, quickLink, appsLink, Messages,SideProfile };
