/*
 * Copyright (C) 2023 DeDiamondPro. - All Rights Reserved
 */

package dev.dediamondpro.resourcify.gui.browsepage

import dev.dediamondpro.resourcify.constraints.ChildLocationSizeConstraint
import dev.dediamondpro.resourcify.constraints.MaxComponentConstraint
import dev.dediamondpro.resourcify.constraints.WindowMinConstraint
import dev.dediamondpro.resourcify.elements.Paginator
import dev.dediamondpro.resourcify.elements.DropDown
import dev.dediamondpro.resourcify.gui.PaginatedScreen
import dev.dediamondpro.resourcify.gui.browsepage.components.ResourceCard
import dev.dediamondpro.resourcify.modrinth.Categories
import dev.dediamondpro.resourcify.modrinth.GameVersions
import dev.dediamondpro.resourcify.modrinth.SearchResponse
import dev.dediamondpro.resourcify.platform.Platform
import dev.dediamondpro.resourcify.util.NetworkUtil
import dev.dediamondpro.resourcify.util.capitalizeAll
import dev.dediamondpro.resourcify.util.getJson
import gg.essential.elementa.components.*
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.universal.UMatrixStack
import org.apache.http.client.utils.URIBuilder
import java.awt.Color
import java.util.concurrent.CompletableFuture
import kotlin.math.ceil

class BrowseScreen : PaginatedScreen() {

    private var offset = 0
    private val selectedCategories = mutableListOf<Categories>()
    private var fetchingFuture: CompletableFuture<Void>? = null
    private var totalHits: Int = 0

    private val contentBox = UIContainer().constrain {
        x = CenterConstraint()
        y = 4.pixels()
        width = ChildBasedSizeConstraint(padding = 4f)
        height = 100.percent()
    } childOf window

    private val sideContainer = UIContainer().constrain {
        x = 0.pixels()
        y = 0.pixels()
        width = 160.pixels()
        height = 100.percent()
    } childOf contentBox

    private val sideBoxScrollable = ScrollComponent(pixelsPerScroll = 30f, scrollAcceleration = 1.5f).constrain {
        x = 0.pixels()
        y = 33.pixels()
        width = 160.pixels()
        height = 100.percent() - 37.pixels()
    } childOf sideContainer

    private val categoryContainer = UIBlock(color = Color(0, 0, 0, 100)).constrain {
        x = 0.pixels()
        y = 0.pixels()
        width = 160.pixels()
        height = ChildLocationSizeConstraint()
    } childOf sideBoxScrollable

    private val mainBox = UIContainer().constrain {
        x = 0.pixels(alignOpposite = true)
        y = 0.pixels()
        width = WindowMinConstraint(528.pixels())
        height = 100.percent()
    } childOf contentBox

    private val headerBox = UIBlock(color = Color(0, 0, 0, 100)).constrain {
        x = 0.pixels()
        y = 0.pixels()
        width = 100.percent()
        height = 29.pixels()
    } childOf mainBox

    private lateinit var searchBox: UITextInput

    private val projectScrollable = ScrollComponent(pixelsPerScroll = 30f, scrollAcceleration = 1.5f).constrain {
        x = 0.pixels()
        y = 33.pixels()
        width = 100.percent()
        height = 100.percent() - 37.pixels()
    } childOf mainBox

    private val projectContainer = UIContainer().constrain {
        x = 0.pixels()
        y = SiblingConstraint(padding = 4f)
        width = 100.percent()
        height = ChildLocationSizeConstraint()
    } childOf projectScrollable

    private var versionDropDown: DropDown? = null
    private var sortDropDown: DropDown? = null

    init {
        sideBar()
        header()
        loadPacks()
    }

    private fun sideBar() {
        Paginator(this).constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 160.pixels()
            height = 29.pixels()
        } childOf sideContainer

        val categoriesBox = UIContainer().constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 100.percent()
            height = ChildLocationSizeConstraint()
        } childOf categoryContainer
        Categories.getCategoriesByHeaderWhenLoaded { categoriesHeaders ->
            for ((header, categories) in categoriesHeaders) {
                UIText(header.capitalizeAll()).constrain {
                    x = 4.pixels()
                    y = MaxConstraint(4.pixels(), SiblingConstraint(padding = 4f))
                    textScale = 1.5f.pixels()
                } childOf categoriesBox

                for (category in categories) {
                    val checkBox = UIContainer().constrain {
                        x = 0.pixels()
                        y = 0.pixels()
                        width = 7.pixels()
                        height = 7.pixels()
                    } effect OutlineEffect(Color.LIGHT_GRAY, 1f)

                    val check = UIBlock(Color(192, 192, 192, 0)).constrain {
                        x = 1.pixels()
                        y = 1.pixels()
                        width = 5.pixels()
                        height = 5.pixels()
                    } childOf checkBox

                    val categoryBox = UIContainer().constrain {
                        x = 4.pixels()
                        y = SiblingConstraint(4f)
                        width = ChildBasedSizeConstraint(4f)
                        height = ChildBasedMaxSizeConstraint()
                    }.onMouseClick {
                        if (it.mouseButton != 0) return@onMouseClick
                        if (selectedCategories.contains(category)) {
                            selectedCategories.remove(category)
                            check.animate {
                                setColorAnimation(
                                    Animations.IN_OUT_QUAD,
                                    0.15f,
                                    Color(192, 192, 192, 0).toConstraint(),
                                    0f
                                )
                            }
                        } else {
                            selectedCategories.add(category)
                            check.animate {
                                setColorAnimation(
                                    Animations.IN_OUT_QUAD,
                                    0.15f,
                                    Color(192, 192, 192, 255).toConstraint(),
                                    0f
                                )
                            }
                        }
                        loadPacks()
                    } childOf categoriesBox
                    checkBox childOf categoryBox

                    UIText(category.name.capitalizeAll()).constrain {
                        x = SiblingConstraint(padding = 4f)
                        y = 0.pixels()
                        color = Color.LIGHT_GRAY.toConstraint()
                    } childOf categoryBox
                }
            }
        }
        val versionsBox = UIContainer().constrain {
            x = 0.pixels()
            y = SiblingConstraint()
            width = 100.percent()
            height = ChildLocationSizeConstraint()
        } childOf categoryContainer
        GameVersions.getVersionsWhenLoaded {
            UIText("Minecraft Versions").constrain {
                x = 4.pixels()
                y = 0.pixels()
                textScale = 1.5f.pixels()
            } childOf versionsBox
            val currVersion = Platform.getMcVersion()
            versionDropDown = DropDown(
                *it.filter { version -> version.versionType == "release" }
                    .map { version -> version.version }.reversed().toTypedArray(),
                selectedOptions = if (it.any { version -> version.version == currVersion })
                    mutableListOf(currVersion) else mutableListOf(),
                top = true, placeHolder = "Choose Versions"
            ).constrain {
                x = 4.pixels()
                y = SiblingConstraint(padding = 4f)
                width = 100.percent() - 8.pixels()
            }.onSelectionUpdate {
                loadPacks()
            } childOf versionsBox
        }
    }

    private fun header() {
        searchBox = (UITextInput("Search resource packs...").constrain {
            x = 6.pixels()
            y = CenterConstraint()
            width = 100.percent() - 89.pixels()
        }.onUpdate {
            loadPacks()
        }.onMouseClick {
            if (it.mouseButton != 0) return@onMouseClick
            grabWindowFocus()
        } childOf headerBox) as UITextInput
        sortDropDown = DropDown(
            "Relevance",
            "Downloads",
            "Follows",
            "Newest",
            "Updated",
            onlyOneOption = true,
            selectedOptions = mutableListOf("Relevance")
        ).constrain {
            x = 5.pixels(true)
            y = CenterConstraint()
            width = 72.pixels()
        }.onSelectionUpdate {
            loadPacks()
        } childOf headerBox
    }

    private fun loadPacks(clear: Boolean = true) {
        fetchingFuture?.cancel(true)
        fetchingFuture = CompletableFuture.runAsync {
            if (clear) offset = 0
            else offset += 20
            val url = URIBuilder("https://api.modrinth.com/v2/search")
                .setParameter("query", searchBox.getText())
                .setParameter("facets", buildFacets())
                .setParameter("limit", "20")
                .setParameter("offset", "$offset")
                .setParameter("index", sortDropDown!!.selectedOptions.first().lowercase())
                .build()
            val response = url.toURL().getJson<SearchResponse>()
            totalHits = response?.totalHits ?: 0
            val projects = response?.hits ?: return@runAsync
            Window.enqueueRenderOperation {
                if (clear) projectContainer.clearChildren()

                for (i in 0 until ceil(projects.size / 2f).toInt()) {
                    val row = UIContainer().constrain {
                        x = 0.pixels()
                        y = SiblingConstraint(padding = 4f)
                        width = 100.percent()
                        height = ChildBasedMaxSizeConstraint()
                    } childOf projectContainer
                    val constraint = MaxComponentConstraint(ChildLocationSizeConstraint() + 4.pixels())
                    ResourceCard(projects[i * 2]).constrain {
                        x = 0.pixels()
                        y = 0.pixels()
                        width = 50.percent() - 2.pixels()
                        height = constraint
                    } childOf row
                    if (projects.size > i * 2 + 1) ResourceCard(projects[i * 2 + 1]).constrain {
                        x = 0.pixels(true)
                        y = 0.pixels()
                        width = 50.percent() - 2.pixels()
                        height = constraint.createChildConstraint(ChildLocationSizeConstraint() + 4.pixels())
                    } childOf row
                }

                if (clear) projectScrollable.scrollToTop(false)
                fetchingFuture = null
            }
        }
    }

    private fun buildFacets(): String = buildString {
        append("[[\"project_type:resourcepack\"]")
        if (selectedCategories.isNotEmpty()) append(",")
        append(selectedCategories.joinToString(",") { "[\"categories:'${it.name}'\"]" })
        versionDropDown?.selectedOptions?.let {
            if (it.isEmpty()) return@let
            append(",[")
            append(it.joinToString(separator = ",") { version -> "\"versions:$version\"" })
            append("]")
        } ?: run {
            val currVersion = Platform.getMcVersion()
            if (GameVersions.getVersions().any { it.version == currVersion })
                append(",[\"versions:${Platform.getMcVersion()}\"]")
        }
        append("]")
    }

    override fun onDrawScreen(matrixStack: UMatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        if (projectScrollable.verticalOffset + projectScrollable.verticalOverhang < 150 && fetchingFuture == null &&
            offset + 20 < totalHits
        ) {
            loadPacks(false)
        }
        super.onDrawScreen(matrixStack, mouseX, mouseY, partialTicks)
    }

    override fun onScreenClose() {
        super.onScreenClose()
        NetworkUtil.clearCache()
    }
}