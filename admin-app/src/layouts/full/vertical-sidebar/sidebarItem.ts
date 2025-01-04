import {
    MenuIcon,
    CircleIcon,
    CircleOffIcon,
    BrandChromeIcon,
    MoodSmileIcon,
    StarIcon,
    AwardIcon,
    AnkhIcon,
    BooksIcon,
    CalendarEventIcon,
    SunHighIcon,
    BrandAppgalleryIcon,
    PictureInPictureOffIcon,
    PictureInPictureIcon, PhotoIcon, UsersIcon
} from 'vue-tabler-icons';

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
        icon: CalendarEventIcon,
        to: '/bookings'
    },
    {
        title: 'Seasons',
        icon: SunHighIcon,
        to: '/seasons'
    },
    {
        title: 'Galleries',
        icon: PhotoIcon,
        to: '/galleries'
    },
    {
        title: 'Users',
        icon: UsersIcon,
        to: '/users',
        adminOnly: true,
    },
];

export default sidebarItem;
