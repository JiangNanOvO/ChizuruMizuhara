# 第三方与许可声明

水原千鹤自己的代码遵循 MIT（见 LICENSE）。工程里还带着两样**别人家的东西**，
它们的许可是分开的，用之前请读一遍。
部分借鉴**星河**

## 1. 星河岛官方接入库 astraisland-sdk-0.1.0.aar

- 路径：`app/libs/astraisland-sdk-0.1.0.aar`
- 来源：星河岛开发者平台 <https://astraflow.cc/island>
- Required Notice: Copyright 2026 MuYuanXing / AstraIsland
- 许可：**PolyForm Noncommercial License 1.0.0**（随包附带的 LICENSE.txt 为准）

含义很直白：**只能非商业用途**。免费公益、自己用、送人用都可以；
一旦要卖、要接广告、要商用，得先找星流（AstraFlow）拿授权。
本工程把它整包放进 `app/libs`，没有改动其中任何字节。

## 2. LyricON 协议端

- 依赖：`io.github.proify.lyricon:provider:0.1.70`、`io.github.proify.lyricon.lyric:model:0.1.70`
- 来源：Maven Central，由 LyricON（词幕）生态提供
- 用途：把播放器自己已经拿到的歌词按协议推给星流，由星流原生「胶囊歌词」呈现

## 3. 星河 AstraGalaxy

- `lyric/` 目录、歌词相关的界面与偏好设置移植自星河 AstraGalaxy
  （Copyright (c) 2026 2979208016，MIT License）。
- 链接助手按星河岛公开接口重写，不再是原来的内部接入库实现。
- MIT 允许这样改、这样发，只要保留原作者版权声明 —— 已经写进 LICENSE 与本文件。

## 4. 其它

- LSPosed / libxposed API：`io.github.libxposed:api:102.0.0`（Apache-2.0）
- 星河岛是星流（AstraFlow）的产品，水原千鹤与星流官方没有隶属关系，
  是「按公开接口接入的第三方应用」。
