package io.github.mjohnson7cd.audiobook.data.model

import io.github.mjohnson7cd.audiobook.data.sources.plex.model.MediaType

data class PlexLibrary(val name: String, val type: MediaType, val id: String)
