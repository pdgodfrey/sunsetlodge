
export interface menu {
    header?: string;
    title?: string;
    icon?: any;
    to?: string;
    chip?: string;
    chipBgColor?: string;
    chipColor?: string;
    chipVariant?: string;
    chipIcon?: string;
    children?: menu[];
    disabled?: boolean;
    type?: string;
    subCaption?: string;
    adminOnly?: boolean;
}

const sidebarItem: menu[] = [
    { header: 'Admin Sections' },
    {
        title: 'Bookings',
        icon: 'calendar-linear',
        to: '/bookings'
    },
    {
        title: 'Seasons',
        icon: 'sun-fog-outline',
        to: '/seasons'
    },
    {
        title: 'Galleries',
        icon: 'wallpaper-linear',
        to: '/galleries'
    },
    {
        title: 'Users',
        icon: 'user-circle-outline',
        to: '/users',
        adminOnly: true,
    },
];

export default sidebarItem;
