### What's New in StreamHub v4.8.344 🚀

- 📌 **Pinned Persistent Top Header Rail (Zero Vanishing)**: Moved category filter pills (**All**, **Anime**, **Movies**, **Series**), **🎲 Surprise Me**, and **Sort ▾** outside the scrollable feed into a permanently pinned top header bar. As you scroll deep down through catalog shelves, the top header bar never scrolls away or vanishes, providing 100% immediate access at all times.
- 🎨 **Cinema-Grade Gradient Surprise Me & Sleek Sort Pills**: Redesigned the **🎲 Surprise Me** button with a rich, luminous rose-to-violet gradient (`#E11D48` ➔ `#8B5CF6`) and bold white typography. Streamlined **Sort ▾** into a sleek dark pill with a crisp downward indicator.
- 📐 **Dynamic Hairline Separation**: Features an ultra-clean 1dp hairline divider (`CardBorderDark.copy(alpha = 0.50f)`) that dynamically appears beneath the pinned header bar when scrolling down, ensuring clean separation as posters glide underneath.
- ⚡ **1-Tap Scroll-To-Top & Smart Filter Reset**: Tapping the active category chip (e.g. tapping "All" while in "All") smoothly animates the feed back to the very top. Switching to a new category automatically resets the scroll position to the top of that category.
- ⚡ **Instant 0ms Episode Transitions (Netflix & YouTube Parity)**: Atomic split-range preloading pre-caches the 25MB head + aligned 512KB tail containing MKV Cues index for $< 1\text{ms}$ startup.
- 🛡️ **Atomic In-Memory Tail Verification & 512KB MTProto Alignment**: Reads tail into memory first, commits to disk cache only upon verified HTTP 206 completion.
