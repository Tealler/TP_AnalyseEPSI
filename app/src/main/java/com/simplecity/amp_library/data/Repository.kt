package com.simplecity.amp_library.data

import com.simplecity.amp_library.model.Album
import com.simplecity.amp_library.model.AlbumArtist
import com.simplecity.amp_library.model.Genre
import com.simplecity.amp_library.model.InclExclItem
import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.model.Song
import io.reactivex.Observable

interface Repository {

    typealias SongsRepository = ((predicate: ((Song) -> Boolean)?) -> Observable<List<Song>>)
    typealias AlbumsRepository = () -> Observable<List<Album>>
    typealias AlbumArtistsRepository = () -> Observable<List<AlbumArtist>>
    typealias GenresRepository = () -> Observable<List<Genre>>

    interface PlaylistsRepository {

        /**
         * Returns a continuous List of [Playlist]s
         */
        fun getPlaylists(): Observable<List<Playlist>>

        /**
         * Returns a continuous List of [Playlist]s, including user-created playlists. Empty playlists are no returned.
         */
        fun getAllPlaylists(songsRepository: SongsRepository): Observable<MutableList<Playlist>>

        fun deletePlaylist(playlist: Playlist)


        fun getPodcastPlaylist(): Playlist

        fun getRecentlyAddedPlaylist(): Playlist

        fun getMostPlayedPlaylist(): Playlist

        fun getRecentlyPlayedPlaylist(): Playlist
    }

    interface InclExclRepository {

        fun add(inclExclItem: InclExclItem)

        fun addAll(inclExclItems: List<InclExclItem>)

        fun addSong(song: Song)

        fun addAllSongs(songs: List<Song>)

        fun delete(inclExclItem: InclExclItem)

        fun deleteAll()
    }

    interface BlacklistRepository : InclExclRepository {

        fun getBlacklistItems(songsRepository: Repository.SongsRepository): Observable<List<InclExclItem>>
    }

    interface WhitelistRepository : InclExclRepository {

        fun getWhitelistItems(songsRepository: Repository.SongsRepository): Observable<List<InclExclItem>>
    }
}