// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'JSignPdf',
  tagline: 'Sign PDF files for free',
  favicon: 'img/signedpdf.png',

  // Set the production url of your site here
  url: 'https://intoolswetrust.github.io/',
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: '/jsignpdf/',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'intoolswetrust', // Usually your GitHub org/user name.
  projectName: 'jsignpdf', // Usually your repo name.

  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',

  // Even if you don't use internalization, you can use this field to set useful
  // metadata like html lang. For example, if your site is Chinese, you may want
  // to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
//          editUrl:
//            'https://github.com/facebook/docusaurus/tree/main/packages/create-docusaurus/templates/shared/',
        },
        blog: {
          showReadingTime: true,
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
//          editUrl:
//            'https://github.com/facebook/docusaurus/tree/main/packages/create-docusaurus/templates/shared/',
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      // Replace with your project's social card
      image: 'img/jsignpdf-social-card.jpg',
      navbar: {
        title: 'JSignPdf',
        logo: {
          alt: 'JSignPdf Logo',
          src: 'img/signedpdf.png',
        },
        items: [
/**
          {
            type: 'docSidebar',
            sidebarId: 'tutorialSidebar',
            position: 'left',
            label: 'Tutorial',
          },
          */
          {to: '/blog', label: 'Blog', position: 'left'},
          {
            href: 'https://github.com/intoolswetrust/jsignpdf',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              {
                label: 'Guide',
                to: '/docs/guide',
              },
            ],
          },
          {
            title: 'Community',
            items: [
              {
                label: 'Google Group',
                href: 'https://groups.google.com/d/forum/jsignpdf',
              },
              {
                label: 'Stack Overflow',
                href: 'https://stackoverflow.com/questions/tagged/jsignpdf',
              },
//              {
//                label: 'Discord',
//                href: 'https://discordapp.com/invite/docusaurus',
//              },
//              {
//                label: 'Twitter',
//                href: 'https://twitter.com/docusaurus',
 //             },
            ],
          },
          {
            title: 'More',
            items: [
              {
                label: 'Blog',
                to: '/blog',
              },
              {
                label: 'GitHub',
                href: 'https://github.com/intoolswetrust/jsignpdf',
              },
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} Josef Cacek.`,
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
      },
    }),
};

module.exports = config;
