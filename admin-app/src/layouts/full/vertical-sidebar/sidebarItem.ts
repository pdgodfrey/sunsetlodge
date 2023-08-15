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
    // { header: 'Others' },
    // {
    //     title: 'Menu Level',
    //     icon: MenuIcon,
    //     to: '#',
    //     children: [
    //         {
    //             title: 'Level 1',
    //             icon: CircleIcon,
    //             to: '/level1'
    //         },
    //         {
    //             title: 'Level 1',
    //             icon: CircleIcon,
    //             to: '/2level',
    //             children: [
    //                 {
    //                     title: 'Level 2',
    //                     icon: CircleIcon,
    //                     to: '/barry'
    //                 },
    //                 {
    //                     title: 'Level 2',
    //                     icon: CircleIcon,
    //                     to: '/2.2level',
    //                     children: [
    //                         {
    //                             title: 'Level 3',
    //                             icon: CircleIcon,
    //                             to: '/barry'
    //                         }
    //                     ]
    //                 }
    //             ]
    //         }
    //     ]
    // },
    // {
    //     title: 'Disabled',
    //     icon: CircleOffIcon,
    //     disabled: true,
    //     to: '/materialpro'
    // },
    // {
    //     title: 'Sub Caption',
    //     icon: StarIcon,
    //     subCaption: 'This is the subtitle',
    //     to: '/materialpro'
    // },
    // {
    //     title: 'Chip',
    //     icon: AwardIcon,
    //     chip: '9',
    //     chipColor: 'surface',
    //     chipBgColor: 'secondary',
    //     to: '/materialpro'
    // },
    // {
    //     title: 'Outlined',
    //     icon: MoodSmileIcon,
    //     chip: 'outline',
    //     chipColor: 'secondary',
    //     chipVariant: 'outlined',
    //     to: '/materialpro'
    // },
    // {
    //     title: 'External Link',
    //     icon: StarIcon,
    //     to: '/modernize',
    //     type: 'external'
    // }
];

export default sidebarItem;
