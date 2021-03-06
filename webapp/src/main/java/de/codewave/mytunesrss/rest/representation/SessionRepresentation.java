package de.codewave.mytunesrss.rest.representation;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Settings of the current session.
 */
@XmlRootElement
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class SessionRepresentation implements RestRepresentation {

    /**
     * @exclude from swagger docs
     */
    private String myLibraryUri;

    /**
     * @exclude from swagger docs
     */
    private List<String> myTranscoders;

    /**
     * @exclude from swagger docs
     */
    private List<MediaRendererRepresentation> myMediaRenderers;

    /**
     * @exclude from swagger docs
     */
    private List<String> myPermissions;

    /**
     * @exclude from swagger docs
     */
    private Integer mySessionTimeoutMinutes;

    /**
     * @exclude from swagger docs
     */
    private Integer mySearchFuzziness;

    /**
     * Main library URI.
     */
    public String getLibraryUri() {
        return myLibraryUri;
    }

    public void setLibraryUri(String libraryUri) {
        myLibraryUri = libraryUri;
    }

    /**
     * List of available transcoders.
     */
    public List<String> getTranscoders() {
        return myTranscoders;
    }

    public void setTranscoders(List<String> transcoders) {
        myTranscoders = transcoders;
    }

    /**
     * List of available media renderers that can be used for the media player.
     */
    public List<MediaRendererRepresentation> getMediaRenderers() {
        return myMediaRenderers;
    }

    public void setMediaRenderers(List<MediaRendererRepresentation> mediaRenderers) {
        myMediaRenderers = mediaRenderers;
    }

    /**
     * List of permissions of the current user.
     */
    public List<String> getPermissions() {
        return myPermissions;
    }

    public void setPermissions(List<String> permissions) {
        myPermissions = permissions;
    }

    /**
     * Session timeout in minutes, use this interval minus at least a few seconds (for latency reasons) for pinging
     * the server to keep the session alive if necessary.
     */
    public Integer getSessionTimeoutMinutes() {
        return mySessionTimeoutMinutes;
    }

    public void setSessionTimeoutMinutes(Integer sessionTimeoutMinutes) {
        mySessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    /**
     * The configured search fuzziness for the user which is either a value from 0 to 100 or -1 for no default value. In case a value from 0 to 100 is returned,
     * any parameter for the track search is ignored and the returned value is used.
     */
    public Integer getSearchFuzziness() {
        return mySearchFuzziness;
    }

    public void setSearchFuzziness(Integer searchFuzziness) {
        mySearchFuzziness = searchFuzziness;
    }
}
