/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomahawk.libtomahawk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

public class LocalCollection extends Collection {

    private static final String TAG = LocalCollection.class.getName();

    private ContentResolver mResolver;

    private Map<Long, Artist> mArtists;
    private Map<Long, Album> mAlbums;
    private Map<Long, Track> mTracks;

    /**
     * Construct a new LocalCollection and initialize.
     * 
     * @param resolver
     */
    public LocalCollection(ContentResolver resolver) {
        mResolver = resolver;
        mArtists = new HashMap<Long, Artist>();
        mAlbums = new HashMap<Long, Album>();
        mTracks = new HashMap<Long, Track>();

        initializeCollection();
    }

    /**
     * Get all Artist's associated with this Collection.
     */
    @Override
    public List<Artist> getArtists() {
        ArrayList<Artist> artists = new ArrayList<Artist>(mArtists.values());
        Collections.sort(artists, new ArtistComparator());
        return artists;
    }

    /**
     * Get all Album's from this Collection.
     */
    @Override
    public List<Album> getAlbums() {
        ArrayList<Album> albums = new ArrayList<Album>(mAlbums.values());
        Collections.sort(albums, new AlbumComparator());
        return albums;
    }

    /**
     * Return a list of all Tracks from the album.
     */
    @Override
    public List<Track> getTracks() {
        ArrayList<Track> tracks = new ArrayList<Track>(mTracks.values());
        Collections.sort(tracks, new TrackComparator(TrackComparator.COMPARE_ALPHA));
        return tracks;
    }

    /**
     * Returns whether this Collection is a local collection.
     */
    @Override
    public boolean isLocal() {
        return true;
    }

    /**
     * Initialize Tracks.
     */
    private void initializeCollection() {
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM };

        Cursor cursor = mResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
                selection, null, null);

        while (cursor != null && cursor.moveToNext()) {
            Artist artist = mArtists.get(cursor.getLong(5));
            if (artist == null) {
                artist = new Artist(cursor.getLong(5));
                artist.setName(cursor.getString(6));

                mArtists.put(artist.getId(), artist);
                Log.d(TAG, "New Artist: " + artist.toString());
            }

            Album album = mAlbums.get(cursor.getLong(7));
            if (album == null) {
                album = new Album(cursor.getLong(7));
                album.setName(cursor.getString(8));

                String albumsel = MediaStore.Audio.Albums._ID + " == "
                        + Long.toString(album.getId());

                String[] albumproj = { MediaStore.Audio.Albums.ALBUM_ART,
                        MediaStore.Audio.Albums.FIRST_YEAR, MediaStore.Audio.Albums.LAST_YEAR };

                Cursor albumcursor = mResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        albumproj, albumsel, null, null);

                if (albumcursor != null && albumcursor.moveToNext()) {

                    album.setAlbumArt(albumcursor.getString(0));
                    album.setFirstYear(albumcursor.getString(1));
                    album.setLastYear(albumcursor.getString(2));

                    mAlbums.put(album.getId(), album);
                    Log.d(TAG, "New Album: " + album.toString());
                }

                albumcursor.close();
            }

            Track track = mTracks.get(cursor.getLong(0));
            if (track == null) {
                track = new Track(cursor.getLong(0));
                track.setPath(cursor.getString(1));
                track.setTitle(cursor.getString(2));
                track.setDuration(cursor.getLong(3));
                track.setTrackNumber(cursor.getInt(4));

                mTracks.put(track.getId(), track);
                Log.d(TAG, "New Track: " + track.toString());
            }

            artist.addAlbum(album);
            artist.addTrack(track);

            album.addTrack(track);
            album.setArtist(artist);

            track.setAlbum(album);
            track.setArtist(artist);
        }

        cursor.close();
    }

    /**
     * Update this Collection's content.
     */
    @Override
    public void update() {
        initializeCollection();
    }
}
