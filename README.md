![Pink logo with pink shark](mastodon/src/main/res/mipmap-xhdpi/ic_launcher_round.png)

# Megalodon

[![Translation status](https://translate.codeberg.org/widgets/megalodon/-/svg-badge.svg)](https://translate.codeberg.org/engage/megalodon/)

> A fork of the [official Mastodon Android app](https://github.com/mastodon/mastodon-android) adding important features that are missing in the official app and possibly won’t ever be implemented, such as the federated timeline, unlisted posting, bookmarks and an image description viewer.

**Warning! [A previous version's integrated updater was broken](https://github.com/sk22/megalodon/issues/106) – I already published a fixed version! If you're not updating through Izzy's F-Droid repository (more sources to come, hopefully!), you'll have to download the current release manually. Sorry about that!**

[![Download latest release](https://img.shields.io/badge/dynamic/json?color=d92aad&label=download%20apk&query=%24.tag_name&url=https%3A%2F%2Fapi.github.com%2Frepos%2Fsk22%2Fmastodon-android-fork%2Freleases%2Flatest&style=for-the-badge)](https://github.com/sk22/megalodon/releases/latest/download/megalodon.apk)

---


## Key features

### **Unlisted posting**

**Allows you to post publicly without having your post show up in trends, hashtags or public timelines (i.e., in the tabs “Local”, “Community” and “Posts”).**

When posting with Unlisted visibility, your posts will still be publicly accessible in your profile. They will also be shown in people’s Home timelines, but only if they follow you or someone they follow reposted/replied to your post.
  
The Mastodon documentation has some more information about [Unlisted posting](https://docs.joinmastodon.org/user/posting/#unlisted) and [Public timelines](https://docs.joinmastodon.org/user/network/#timelines).

### **Federated timeline**

**This allows you to chronologically see all Public posts from people on all other Fediverse neighborhoods your home instance is connected to.**

Despite being one of the main features of federated social media, the Federated timeline wasn’t included in the official Mastodon app – supposedly, because this conflicts with Google’s safety requirements for apps on the Play Store.
  
That’s one of the reasons why choosing a small, **well-moderated instance is important**. Instance admins and moderators should always make sure to ban abusive users and stop federating with instances who platform them. On well-moderated instances, the Federated timeline can be a welcoming place to meet new people!

### **Image description viewer**

**Allows you to quickly check whether an image or video has an alternative text attached to it.**

This is important to **ensure the content you’re sharing is as accessible as possible** to people who can’t see the images and rely on software to read back the provided content descriptions. Thankfully, it’s quite common for people on the Fediverse to provide such alt texts, and hopefully things stay this way!

### **Pinning posts**

**This lets you can highlight important posts on your profile. A dedicated “Pinned” tab in people’s profiles shows all the posts they pinned.**

On the Fediverse, it’s quite common for people to pin posts they want others to read before following them. You can pin/unpin posts yourself by clicking the `⋯` button in the top right corner of your posts.

### **Bookmarks**

**They allow for quickly saving posts and viewing them through the Bookmarks button on the top right of your profile.**

To bookmark a post, press the button between the Favorite and Share buttons on the bottom of the post. Bookmarks are saved privately, so the post authors won’t know you saved their post – the list of bookmarked posts is only visible to you.

## Installation

**In short: Press the download button to download the APK. Open the downloaded file on your Android device to install it. Megalodon will automatically notify you about new updates inside the app.**

[![Download latest release](https://img.shields.io/badge/dynamic/json?color=d92aad&label=download%20apk&query=%24.tag_name&url=https%3A%2F%2Fapi.github.com%2Frepos%2Fsk22%2Fmastodon-android-fork%2Freleases%2Flatest&style=for-the-badge)](https://github.com/sk22/megalodon/releases/latest/download/megalodon.apk)

To install this app on your Android device, download the [latest release from GitHub](https://github.com/sk22/megalodon/releases/latest/download/megalodon.apk) and open it. You might have to accept installing APK files from your browser when trying to install it. You can also take a look at all releases on the [Releases](https://github.com/sk22/megalodon/releases) page.

Megalodon makes use of [Mastodon for Android](https://github.com/mastodon/mastodon-android)’s automatic update checker. Megalodon will check for new updates available on GitHub and offer to download and install them. You can also manually press “Check for updates” at the bottom of the settings page!

### Other sources

* **[Izzy's F-Droid repository](https://apt.izzysoft.de/fdroid/repo)**: https://apt.izzysoft.de/fdroid/index/apk/org.joinmastodon.android.sk

---


## Release variants

All downloads can be found on the [Releases](https://github.com/sk22/megalodon/releases) page.

**`megalodon.apk`**

Variant with an integrated updater. If you download Megalodon from here (and not from an app store), just download the regular `megalodon.apk`.

**`upstream-1234abc.apk`**

This is an **unmodified version** of the official [Mastodon for Android](https://github.com/mastodon/mastodon-android) app the respective Megalodon release is based on. Should you find any bugs in Megalodon (which you will), try to see if it occurs with this variant, too. The last 7 digits of the file name are important to know which version of the official app you're using.

<!-- **`megalodon-fdroid.apk`**

Variant without the integrated updater. This is the variant to be published to F-Droid.org where an integrated updater is not necessary. -->

---

## Contribution

### Translation

As with the source code, the translation is sourced from the official project, which you can contribute to on the official “**Mastodon for Android**” Crowdin project: https://crowdin.com/project/mastodon-for-android

There's also a handful of custom strings exclusive to this projects that would need to be translated. You can help translate **Megalodon** on Weblate: https://translate.codeberg.org/projects/megalodon/

[![Translation status](https://translate.codeberg.org/widgets/megalodon/-/multi-auto.svg)](https://translate.codeberg.org/engage/megalodon/)


---


## Detailed changes

### Features

* [Add “Unlisted” as a post visibility option](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/enable-unlisted)
  ([Pull request](https://github.com/mastodon/mastodon-android/pull/103))
* [Add “Federation” tab and change Discover tab order](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/add-federated-timeline) ([Closes issue](https://github.com/mastodon/mastodon-android/issues/8))
* [Add image description button and viewer](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/display-alt-text) ([Pull request](https://github.com/mastodon/mastodon-android/pull/129))
* [Implement pinning posts and displaying pinned posts](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/pin-posts) ([Pull request](https://github.com/mastodon/mastodon-android/pull/140))
* [Implement deleting and re-drafting](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/delete-redraft) ([Closes issue](https://github.com/mastodon/mastodon-android/issues/21))
* [Implement a bookmark button and list](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/bookmarks) ([Closes issue](https://github.com/mastodon/mastodon-android/issues/22))
* [Add “Check for update” button in addition to integrated update checker](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/check-for-update-button)
* [Add “Mark media as sensitive” option](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/mark-media-as-sensitive)
* [Add settings to hide replies and reposts from the timeline](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/filter-home-timeline) ([Pull request](https://github.com/mastodon/mastodon-android/pull/317))
* [Follow and unfollow hashtags](https://github.com/sk22/megalodon/commit/7d38f031f197aa6cefaf53e39d929538689c1e4e) ([Closes issue](https://github.com/mastodon/mastodon-android/issues/233))
* [Notification bell for posts](https://github.com/sk22/megalodon/commit/b166ca705eb9169025ef32bbe6315b42491b57ea) ([Closes issue](https://github.com/mastodon/mastodon-android/issues/81))
* [Viewing lists and adding/removing users from lists](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:list-timeline-views) based on [@obstsalatschuessel](https://github.com/obstsalatschuessel)'s [Pull request](https://github.com/mastodon/mastodon-android/pull/286)
* [List favorited posts](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/favs-list)
* [Accept/reject follow requests](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/follow-requests)
* [Display content warning title above text](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/cw-above-text)
* [Add notifications tab for posts](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/posts-notifications-tab)
* [Show visibility of original post when replying](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/display-reply-visibility)
* [Clickable reply/boost line above posts](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:clickable-boost-reply-line)
* [Clickable reply line while replying to open original post](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/clickable-reply-line-compose)


### Behavior

* [Make back button return to the home tab before exiting the app](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/back-returns-home) ([Closes issue](https://github.com/mastodon/mastodon-android/issues/118))
* [Always preserve content warnings when replying](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/always-preserve-cw) ([Closes issue](https://github.com/mastodon/mastodon-android/issues/113))
* [Display full image when adding image description](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/compose-image-description-full-image) ([Pull request](https://github.com/mastodon/mastodon-android/pull/182))
* [Set spoiler height independently to content height](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:spoiler-height-independent) ([Closes issue](https://github.com/mastodon/mastodon-android/issues/166))
* [Option to hide interaction numbers](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:settings/hide-interaction-numbers)
* [Option to always reveal content warnings](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/cw-above-text)
* [Option to disable scrolling title bars](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:settings/disable-marquee)


### Visual

* [Custom extended footer redesign](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:compact-extended-footer)
* [Improvements to the true black mode](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:true-black-improvements)
* [Profile header tweaks](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:ui/profile-header-tweaks)


## Building

As this app is using Java 17 features, you need JDK 17 or newer to build it. Other than that, everything is pretty standard. You can either import the project into Android Studio and build it from there, or run the following command in the project directory:

```
./gradlew assembleRelease
```

## License

This project is released under the [GPL-3 License](./LICENSE).

## Links

<a rel="me" href="https://floss.social/@megalodon">@megalodon<wbr>@floss.social</a>
