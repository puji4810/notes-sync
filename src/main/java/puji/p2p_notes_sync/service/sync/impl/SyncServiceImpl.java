package puji.p2p_notes_sync.service.sync.impl;

import puji.p2p_notes_sync.model.sync.SyncState;
import puji.p2p_notes_sync.model.sync.SyncStatus;
import puji.p2p_notes_sync.p2p.coordinator.P2PCoordinatorService;
import puji.p2p_notes_sync.service.config.ConfigService;
import puji.p2p_notes_sync.service.git.GitService;
import puji.p2p_notes_sync.service.sync.SyncService;
import puji.p2p_notes_sync.service.sync.SyncStatusStorage;
import puji.p2p_notes_sync.model.config.SyncConfig;
import puji.p2p_notes_sync.model.sync.SyncStrategy;
import puji.p2p_notes_sync.model.sync.ConflictResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// ... existing code ...