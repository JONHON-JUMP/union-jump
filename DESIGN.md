# Design System

## Theme

浅色工业工作台。背景使用低饱和蓝灰，主要内容使用白色和浅蓝表面，品牌强调色为 JUMP 蓝。桌面图标允许使用受控的业务分类色，但所有选中态和主要操作统一使用品牌蓝。

## Color

- Primary: `#087CE5`
- Primary dark: `#075EB5`
- Ink: `#10233E`
- Secondary text: `#5D718C`
- Canvas: `#EAF4FC`
- Surface: `#FFFFFF`
- Soft surface: `#E4F0FA`
- Success: `#0D9F6E`
- Warning: `#E88A08`
- Danger: `#E5484D`

## Typography

使用项目现有系统字体栈：`PingFang SC`, `Microsoft YaHei`, `Helvetica Neue`, Arial, sans-serif。标题使用 600 至 700 字重，正文使用 400 至 500 字重。数字使用等宽数字特性以提升概览数据的稳定感。

## Shape

卡片圆角 16px，输入框和按钮圆角 12px，状态标签使用完整胶囊圆角。应用图标为 22px 圆角方形。阴影只用于浮层、Dock 和应用图标，不与装饰性描边叠加。

## Layout

首页采用顶部 Header、中央主工作区、右侧信息栏和底部 Dock。宽屏为双栏结构，低于 1180px 时右侧信息栏移动到应用区下方，低于 760px 时应用图标缩小并改为紧凑网格。

## Motion

交互过渡控制在 160ms 至 240ms，仅用于悬停、按下、抽屉和状态切换。应用图标悬停轻微上移，按下缩放。用户选择减少动态效果时关闭位移与动画。

## Components

- Header: 品牌、搜索、当前子系统、通知和用户菜单。
- App tile: 彩色圆角图标、名称、可选角标。
- Overview: 三项关键状态，颜色与文字共同表达。
- Workbench: Element-UI 页签式任务列表。
- Dock: 五个固定入口，当前项使用品牌蓝。
- App drawer: Element-UI Drawer，展示 mock 应用和权限路由菜单。
