![Pink logo with pink shark](mastodon/src/main/res/mipmap-xhdpi/ic_launcher_round.png)

# Megalodon

[![Translation status](https://translate.codeberg.org/widgets/megalodon/-/svg-badge.svg)](https://translate.codeberg.org/engage/megalodon/)
&nbsp;
[![Download latest release](https://img.shields.io/badge/dynamic/json?color=d92aad&label=Download%20APK&query=%24.tag_name&url=https%3A%2F%2Fapi.github.com%2Frepos%2Fsk22%2Fmegalodon%2Freleases%2Flatest&style=flat)](https://github.com/sk22/megalodon/releases/latest/download/megalodon.apk)

<a href="https://play.google.com/store/apps/details?id=org.joinmastodon.android.sk"><img height="50" alt="Get it on Google Play" src="img/google-play-badge.png"></a>
&nbsp;
<a href="https://apt.izzysoft.de/fdroid/index/apk/org.joinmastodon.android.sk"><img height="50" alt="Get it on F-Droid" src="img/f-droid-badge.png"></a>

> A fork of the [official Mastodon Android app](https://github.com/mastodon/mastodon-android) adding important features that are missing in the official app and possibly won’t ever be implemented, such as the federated timeline, unlisted posting and an image description viewer.


## Key features

### **Unlisted posting**

**Allows you to post publicly without having your post show up in trends, hashtags or public timelines (i.e., in the tabs “Community”, “Federated” and “Posts”).**

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

## Installation

### From app stores

* **[Izzy's F-Droid repository](https://apt.izzysoft.de/fdroid/repo)**: [apt.izzysoft.de/fdroid/index/apk/org.joinmastodon.android.sk](https://apt.izzysoft.de/fdroid/index/apk/org.joinmastodon.android.sk)

  Note that you'll need to add Izzy's F-Droid repository to your F-Droid app first:
  
  `https://apt.izzysoft.de/fdroid/repo`

* **[Google Play Store](https://play.google.com/store/apps/details?id=org.joinmastodon.android.sk)**: [play.google.com/store/apps/details?id=org.joinmastodon.android.sk](https://play.google.com/store/apps/details?id=org.joinmastodon.android.sk)

* **[F-Droid.org](https://f-droid.org)?** Not yet, sorry!

  If you want, you can help me figure out if something's missing in the [Issue #47: F-Droid.org](https://github.com/sk22/megalodon/issues/47)

### Directly from GitHub

Press the download button to download the APK. Open the downloaded file on your Android device to install it. Megalodon will automatically notify you about new updates inside the app.

[![Download latest release](https://img.shields.io/badge/dynamic/json?color=d92aad&label=Download%20APK&query=%24.tag_name&url=https%3A%2F%2Fapi.github.com%2Frepos%2Fsk22%2Fmegalodon%2Freleases%2Flatest&style=flat)](https://github.com/sk22/megalodon/releases/latest/download/megalodon.apk)

You might have to accept installing APK files from your browser when trying to install it. You can also take a look at all releases on the [Releases](https://github.com/sk22/megalodon/releases) page.

Megalodon makes use of [Mastodon for Android](https://github.com/mastodon/mastodon-android)’s automatic update checker. Megalodon will check for new updates available on GitHub and offer to download and install them. You can also manually press “Check for updates” at the bottom of the settings page!

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

[![Translation status](https://translate.codeberg.org/widgets/megalodon/-/horizontal-auto.svg)](https://translate.codeberg.org/engage/megalodon/)


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
* [Add push notification setting for post notifications](https://github.com/sk22/megalodon/commit/b190480d7739be47f23543d9e7644660f9b4b4ee)
* [Add option to allow voting for multiple options on polls](https://github.com/sk22/megalodon/commit/5b28468efd49387b4f8b83f142f3adf3104ca60c)
* [Add translate function](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/translate-button)
* [Add language selector](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/language-selector)
* [Implement deleting notifications](https://github.com/sk22/megalodon/commit/b0f9ce081f69f29ad59658fc00ca41372cd2677d) (disabled by default)


### Behavior

* [Make back button return to the home tab before exiting the app](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/back-returns-home) ([Closes issue](https://github.com/mastodon/mastodon-android/issues/118))
* [Always preserve content warnings when replying](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/always-preserve-cw) ([Closes issue](https://github.com/mastodon/mastodon-android/issues/113))
* [Display full image when adding image description](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/compose-image-description-full-image) ([Pull request](https://github.com/mastodon/mastodon-android/pull/182))
* [Set spoiler height independently to content height](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:spoiler-height-independent) ([Closes issue](https://github.com/mastodon/mastodon-android/issues/166))
* [Option to hide interaction numbers](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:settings/hide-interaction-numbers)
* [Option to always reveal content warnings](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/cw-above-text)
* [Option to disable scrolling title bars](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:settings/disable-marquee)
* [No ellipsis for long poll answers](https://github.com/mastodon/mastodon-android/commit/c9aae828e2518adccdc092e41f8d1f0489636271)
* [Show poll vote button for multiple and single answer polls](https://github.com/mastodon/mastodon-android/commit/e14dfda2fdf32f0fa3043504ac5831683a87559a)
* [Show own vote after voting](https://github.com/mastodon/mastodon-android/commit/4ab9e25fec4fd9c10b7a8ddd1be522b3cc12cf28) ([Closes issue](https://github.com/mastodon/mastodon-android/commit/4ab9e25fec4fd9c10b7a8ddd1be522b3cc12cf28))
* [Make inline emoji search case-insensitive and don't only search from start of emoji names](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:better-inline-emoji-search) ([Pull request](https://github.com/mastodon/mastodon-android/pull/445))
* [Include subject line when sharing e.g. a website to Megalodon](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:external-share-include-subject)
* [Improve semantics for voting on polls (radio buttons and checkboxes)](https://github.com/sk22/megalodon/commit/6fd58c96827cb1d2da329cebdc170a1425dd18d7)
* [Copy post URL when long-pressing share button](https://github.com/sk22/megalodon/commit/ba36347f03278763ecec617b1ce57ba89db7be72)
* [Add option to disable swiping between tabs](https://github.com/sk22/megalodon/commit/1f20b21fc84bf006c1ec14bd2229cbfad5215ec8)


### Visual

* [Custom extended footer redesign](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:compact-extended-footer)
* [Improvements to the true black mode](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:true-black-improvements)
* [Profile header tweaks](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:ui/profile-header-tweaks)
* [Custom color themes](https://github.com/sk22/megalodon/pull/124) by [@LucasGGamerM](https://github.com/LucasGGamerM)
* [Custom "megalodon" text logo](https://github.com/sk22/megalodon/commit/563afd487ca5c608cfbb00fa3909d3c27384acc0) by [@LucasGGamerM](https://github.com/LucasGGamerM)
* [Custom login screen](https://github.com/sk22/megalodon/commit/9bbf8c4618dbe13accaeb3b5482bf3fe88cac4c0)
* [More distinct filled boost icon](https://github.com/sk22/megalodon/commits/more-distinct-filled-boost-icon)
* Material You color theme by [@LucasGGamerM](https://github.com/LucasGGamerM)
* [Animations for interaction buttons](https://github.com/mastodon/mastodon-android/compare/master...sk22:megalodon:feature/animate-buttons)


## Building

As this app is using Java 17 features, you need JDK 17 or newer to build it. Other than that, everything is pretty standard. You can either import the project into Android Studio and build it from there, or run the following command in the project directory:

```
./gradlew assembleRelease
```

## License

This project is released under the [GPL-3 License](./LICENSE).

## Links

<a rel="me" href="https://floss.social/@megalodon">@megalodon<wbr>@floss.social</a>
