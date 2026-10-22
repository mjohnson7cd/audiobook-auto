package io.github.mjohnson7cd.audiobook.features.player

import android.support.v4.media.MediaMetadataCompat
import io.github.mjohnson7cd.audiobook.data.model.MediaItemTrack
import io.github.mjohnson7cd.audiobook.data.model.toMediaMetadata
import io.github.mjohnson7cd.audiobook.data.sources.plex.PlexConfig

fun buildPlaylist(
    tracks: List<MediaItemTrack>,
    plexConfig: PlexConfig,
): List<MediaMetadataCompat> {
    return tracks.map { track -> track.toMediaMetadata(plexConfig) }
}
