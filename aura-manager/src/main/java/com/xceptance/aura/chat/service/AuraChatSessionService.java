/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.aura.chat.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xceptance.aura.chat.dto.ChatMessageDto;
import com.xceptance.aura.chat.dto.ChatSessionDto;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Thread-safe service managing chat session file persistence, CRUD operations,
 * message additions, and disk-backed JSON serialization in {@code chat-history/}.
 *
 * @author AI-generated: Gemini 4 Hydrogen
 * @author Xceptance GmbH 2026
 */
@Service
public class AuraChatSessionService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraChatSessionService.class);
    private static final Pattern SAFE_SESSION_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,128}$");
    private static final int MAX_SESSION_NAME_LENGTH = 80;

    private final File chatHistoryDir;
    private final Gson gson;
    private final Map<String, ChatSessionDto> sessionCache;

    /**
     * Constructs a session service persisting history under {@code <user.dir>/chat-history}.
     */
    public AuraChatSessionService()
    {
        this(resolveHistoryDir(null));
    }

    /**
     * Constructs a session service using an optional Spring-configured history directory.
     *
     * @param historyDir configured history directory path, or {@code null}/blank for {@code <user.dir>/chat-history}
     */
    @Autowired
    public AuraChatSessionService(@Value("${aura.chat.history-dir:#{null}}") final String historyDir)
    {
        this(resolveHistoryDir(historyDir));
    }

    /**
     * Constructs a session service persisting history under the specified directory.
     *
     * @param chatHistoryDir directory used for JSON session files
     */
    public AuraChatSessionService(final File chatHistoryDir)
    {
        this.chatHistoryDir = chatHistoryDir != null ? chatHistoryDir : resolveHistoryDir(null);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.sessionCache = new ConcurrentHashMap<>();
        ensureDirectoryExists();
    }

    private static File resolveHistoryDir(final String configuredDir)
    {
        if (configuredDir != null && !configuredDir.isBlank())
        {
            return new File(configuredDir.trim());
        }
        return new File(System.getProperty("user.dir"), "chat-history");
    }

    private void ensureDirectoryExists()
    {
        if (!this.chatHistoryDir.exists())
        {
            this.chatHistoryDir.mkdirs();
        }
    }

    private File resolveSafeSessionFile(final String id)
    {
        if (id == null || !SAFE_SESSION_ID_PATTERN.matcher(id).matches())
        {
            throw new IllegalArgumentException("Invalid chat session identifier.");
        }
        final Path basePath = this.chatHistoryDir.toPath().toAbsolutePath().normalize();
        final Path resolvedPath = basePath.resolve(id + ".json").normalize();
        if (!resolvedPath.startsWith(basePath) || !resolvedPath.getParent().equals(basePath))
        {
            throw new IllegalArgumentException("Chat session path escapes history directory.");
        }
        return resolvedPath.toFile();
    }

    private static ChatSessionDto snapshotOf(final ChatSessionDto source)
    {
        if (source == null)
        {
            return null;
        }
        final List<ChatMessageDto> copiedMessages = source.messages != null
                ? List.copyOf(source.messages)
                : List.of();
        return new ChatSessionDto(source.id, source.name, copiedMessages, source.updatedAt);
    }

    /**
     * Lists all persisted chat sessions, sorted by most recently updated first (name tie-breaker).
     * Guaranteed to return at least one "Default Session" if none are found.
     *
     * @return sorted list of defensive chat session snapshots
     */
    public synchronized List<ChatSessionDto> getSessions()
    {
        ensureDirectoryExists();
        final List<ChatSessionDto> sessions = new ArrayList<>();
        final File[] files = this.chatHistoryDir.listFiles((dir, name) -> name.endsWith(".json"));

        if (files != null && files.length > 0)
        {
            final Set<String> diskIds = new HashSet<>();
            for (final File file : files)
            {
                try
                {
                    final String fileName = file.getName();
                    final String candidateId = fileName.endsWith(".json")
                            ? fileName.substring(0, fileName.length() - 5)
                            : fileName;
                    if (SAFE_SESSION_ID_PATTERN.matcher(candidateId).matches())
                    {
                        final ChatSessionDto cached = this.sessionCache.get(candidateId);
                        if (cached != null)
                        {
                            diskIds.add(candidateId);
                            sessions.add(snapshotOf(cached));
                            continue;
                        }
                    }

                    final String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                    final ChatSessionDto session = this.gson.fromJson(content, ChatSessionDto.class);
                    if (session != null && session.id != null && SAFE_SESSION_ID_PATTERN.matcher(session.id).matches())
                    {
                        normalizeLoadedSession(session, file);
                        diskIds.add(session.id);
                        final ChatSessionDto cached = this.sessionCache.get(session.id);
                        if (cached != null)
                        {
                            sessions.add(snapshotOf(cached));
                        }
                        else
                        {
                            this.sessionCache.put(session.id, session);
                            sessions.add(snapshotOf(session));
                        }
                    }
                }
                catch (final Exception e)
                {
                    LOGGER.error("[Aura Chat] Failed to load chat session file: {}", file.getName(), e);
                }
            }
            this.sessionCache.keySet().removeIf(id -> !diskIds.contains(id));
        }
        else
        {
            this.sessionCache.clear();
        }

        if (sessions.isEmpty())
        {
            final ChatSessionDto defaultSession = createSession("Default Session");
            sessions.add(defaultSession);
        }

        sessions.sort((s1, s2) -> {
            final int recency = Long.compare(s2.updatedAt, s1.updatedAt);
            return recency != 0 ? recency : s1.name.compareToIgnoreCase(s2.name);
        });
        return sessions;
    }

    private static void normalizeLoadedSession(final ChatSessionDto session, final File file)
    {
        if (session.messages == null)
        {
            session.messages = new ArrayList<>();
        }
        if (session.name == null || session.name.isBlank())
        {
            session.name = "Chat";
        }
        if (session.updatedAt <= 0L)
        {
            final long modified = file.lastModified();
            session.updatedAt = modified > 0L ? modified : System.currentTimeMillis();
        }
    }

    /**
     * Looks up a specific chat session by exact ID without falling back to another session.
     *
     * @param id session identifier
     * @return optional matching session snapshot, or empty if null/blank/missing
     * @throws IllegalArgumentException if {@code id} is non-blank and fails safety validation
     */
    public synchronized Optional<ChatSessionDto> findSession(final String id)
    {
        return findInternalSession(id).map(AuraChatSessionService::snapshotOf);
    }

    private Optional<ChatSessionDto> findInternalSession(final String id)
    {
        if (id == null || id.isBlank())
        {
            return Optional.empty();
        }

        final File file = resolveSafeSessionFile(id);
        final ChatSessionDto cached = this.sessionCache.get(id);
        if (cached != null)
        {
            return Optional.of(cached);
        }

        if (file.exists())
        {
            try
            {
                final String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                final ChatSessionDto session = this.gson.fromJson(content, ChatSessionDto.class);
                if (session != null && id.equals(session.id))
                {
                    normalizeLoadedSession(session, file);
                    this.sessionCache.put(session.id, session);
                    return Optional.of(session);
                }
            }
            catch (final Exception e)
            {
                LOGGER.error("[Aura Chat] Failed to load chat session: {}", id, e);
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves a specific chat session snapshot by ID, falling back read-only to the first available
     * session when {@code id} is null, blank, or unknown.
     *
     * @param id session identifier
     * @return matching session snapshot or default fallback snapshot
     * @throws IllegalArgumentException if {@code id} is non-blank and violates ID format rules
     */
    public synchronized ChatSessionDto getSession(final String id)
    {
        if (id == null || id.isBlank())
        {
            return getSessions().get(0);
        }

        final Optional<ChatSessionDto> exactMatch = findSession(id);
        return exactMatch.orElseGet(() -> getSessions().get(0));
    }

    /**
     * Creates and persists a new chat session on disk.
     *
     * @param name display name for the session
     * @return defensive snapshot of newly created session
     */
    public synchronized ChatSessionDto createSession(final String name)
    {
        ensureDirectoryExists();
        final String cleanName = normalizeSessionName(name, "New Chat");
        final String id = "s-" + UUID.randomUUID().toString().substring(0, 8) + "-" + System.currentTimeMillis();

        final ChatSessionDto session = new ChatSessionDto(id, cleanName, new ArrayList<>(), System.currentTimeMillis());
        this.sessionCache.put(session.id, session);
        persistSession(session);
        return snapshotOf(session);
    }

    /**
     * Renames an existing chat session and persists the update.
     * Never falls back to mutating another session if {@code id} does not exist.
     *
     * @param id session identifier
     * @param newName new display name
     * @return updated session snapshot, or {@code null} if session was not found
     * @throws IllegalArgumentException if {@code id} is non-blank and violates safety validation
     */
    public synchronized ChatSessionDto renameSession(final String id, final String newName)
    {
        final Optional<ChatSessionDto> target = findInternalSession(id);
        if (target.isEmpty())
        {
            LOGGER.warn("[Aura Chat] Ignoring rename for missing chat session: {}", id);
            return null;
        }

        final ChatSessionDto session = target.get();
        final String cleanName = normalizeSessionName(newName, null);
        if (cleanName == null)
        {
            return snapshotOf(session);
        }

        session.name = cleanName;
        session.updatedAt = System.currentTimeMillis();
        persistSession(session);
        return snapshotOf(session);
    }

    private static String normalizeSessionName(final String rawName, final String fallback)
    {
        if (rawName == null)
        {
            return fallback;
        }
        final String trimmed = rawName.trim();
        if (trimmed.isEmpty())
        {
            return fallback;
        }
        return trimmed.length() > MAX_SESSION_NAME_LENGTH
                ? trimmed.substring(0, MAX_SESSION_NAME_LENGTH)
                : trimmed;
    }

    /**
     * Deletes a chat session from cache and disk.
     *
     * @param id session identifier
     * @throws IllegalArgumentException if {@code id} is non-blank and violates safety validation
     */
    public synchronized void deleteSession(final String id)
    {
        if (id == null || id.isBlank())
        {
            return;
        }

        final File file = resolveSafeSessionFile(id);
        this.sessionCache.remove(id);
        try
        {
            Files.deleteIfExists(file.toPath());
        }
        catch (final IOException e)
        {
            LOGGER.error("[Aura Chat] Failed to delete chat session file: {}", id, e);
        }
    }

    /**
     * Appends a new chat message to an existing session and persists it to disk.
     * Never falls back to appending onto another session if {@code id} does not exist.
     *
     * @param id session identifier
     * @param message message to append
     * @throws IllegalArgumentException if {@code id} is non-blank and violates safety validation
     */
    public synchronized void addMessage(final String id, final ChatMessageDto message)
    {
        if (id == null || id.isBlank() || message == null)
        {
            return;
        }

        final Optional<ChatSessionDto> target = findInternalSession(id);
        if (target.isEmpty())
        {
            LOGGER.warn("[Aura Chat] Ignoring message append for missing chat session: {}", id);
            return;
        }

        final ChatSessionDto session = target.get();
        session.messages.add(message);
        session.updatedAt = System.currentTimeMillis();
        persistSession(session);
    }

    private void persistSession(final ChatSessionDto session)
    {
        ensureDirectoryExists();
        final Path targetPath = resolveSafeSessionFile(session.id).toPath();
        final Path parentDir = targetPath.getParent();
        Path tempFile = null;
        try
        {
            final String content = this.gson.toJson(session);
            tempFile = Files.createTempFile(parentDir, session.id + ".", ".json.tmp");
            Files.writeString(tempFile, content, StandardCharsets.UTF_8);
            try
            {
                Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (final AtomicMoveNotSupportedException e)
            {
                Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (final IOException e)
        {
            if (tempFile != null)
            {
                try
                {
                    Files.deleteIfExists(tempFile);
                }
                catch (final IOException cleanupEx)
                {
                    LOGGER.debug("[Aura Chat] Failed cleaning up temp file {}: {}", tempFile, cleanupEx.getMessage());
                }
            }
            LOGGER.error("[Aura Chat] Failed to write chat session file: {}", session.id, e);
        }
    }
}
