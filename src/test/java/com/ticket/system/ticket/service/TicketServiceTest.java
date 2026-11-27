package com.ticket.system.ticket.service;

import com.ticket.system.common.exception.BadRequestException;
import com.ticket.system.common.exception.ConflictException;
import com.ticket.system.common.exception.ForbiddenException;
import com.ticket.system.common.exception.NotFoundException;
import com.ticket.system.security.CustomUserDetails;
import com.ticket.system.ticket.dto.CreateTicketRequest;
import com.ticket.system.ticket.dto.TicketResponse;
import com.ticket.system.ticket.dto.UpdateStatusRequest;
import com.ticket.system.ticket.dto.UpdateTicketRequest;
import com.ticket.system.ticket.entity.Ticket;
import com.ticket.system.ticket.enums.Priority;
import com.ticket.system.ticket.enums.TicketStatus;
import com.ticket.system.ticket.repository.IdempotencyRepository;
import com.ticket.system.ticket.repository.TicketRepository;
import com.ticket.system.ticket.repository.TicketStatusHistoryRepository;
import com.ticket.system.ticket.entity.IdempotencyRecord;
import com.ticket.system.ticket.entity.TicketStatusHistory;
import com.ticket.system.user.entity.User;
import com.ticket.system.user.enums.Role;
import com.ticket.system.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;


@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TicketService ticketService;

    @Mock
    private TicketStatusHistoryRepository historyRepository;

    @Mock
    private IdempotencyRepository idempotencyRepository;

    private final CustomUserDetails user = new CustomUserDetails(1L, "user@test.com", Role.USER);
    private final CustomUserDetails admin = new CustomUserDetails(99L, "admin@test.com", Role.ADMIN);

    private User userEntity(long id) {
        User u = new User();
        u.setId(id);
        u.setEmail("owner" + id + "@test.com");
        u.setRole(Role.USER);
        return u;
    }

    private Ticket ticketOwnedBy(long ownerId) {
        Ticket t = new Ticket();
        t.setId(10L);
        t.setTitle("Wi-Fi not working");
        t.setDescription("Cannot connect");
        t.setOwner(userEntity(ownerId));
        return t;
    }

    private Ticket ticketWithStatus(long ownerId, TicketStatus status) {
        Ticket t = ticketOwnedBy(ownerId);
        t.setStatus(status);
        return t;
    }

    // ---- create, list, get ----

    @Test
    void create_savesTicketAsOpenWithTrimmedTitle() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity(1L)));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));


        TicketResponse response = ticketService.create(
                new CreateTicketRequest("  Wi-Fi  ", "  down  ", Priority.HIGH), null, user);

        assertThat(response.title()).isEqualTo("Wi-Fi");
        assertThat(response.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(response.priority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void listMine_asUser_queriesOnlyOwnTickets() {
        Pageable pageable = PageRequest.of(0, 10);
        when(ticketRepository.findByOwnerId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(ticketOwnedBy(1L))));

        ticketService.listMine(null, pageable, user);

        verify(ticketRepository).findByOwnerId(1L, pageable);
        verify(ticketRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listMine_asAdmin_stillQueriesOnlyTheAdminsOwnTickets() {
        Pageable pageable = PageRequest.of(0, 10);
        when(ticketRepository.findByOwnerId(99L, pageable))
                .thenReturn(new PageImpl<>(List.of(ticketOwnedBy(99L))));

        ticketService.listMine(null, pageable, admin);

        verify(ticketRepository).findByOwnerId(99L, pageable);
        verify(ticketRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listMine_withStatus_filtersOwnTicketsByStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        when(ticketRepository.findByOwnerIdAndStatus(1L, TicketStatus.OPEN, pageable))
                .thenReturn(new PageImpl<>(List.of(ticketOwnedBy(1L))));

        ticketService.listMine(TicketStatus.OPEN, pageable, user);

        verify(ticketRepository).findByOwnerIdAndStatus(1L, TicketStatus.OPEN, pageable);
    }

    @Test
    void listAll_asAdmin_queriesEveryTicket() {
        Pageable pageable = PageRequest.of(0, 10);
        when(ticketRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(ticketOwnedBy(1L))));

        ticketService.listAll(null, pageable, admin);

        verify(ticketRepository).findAll(pageable);
    }

    @Test
    void listAll_withStatus_filtersEveryTicketByStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        when(ticketRepository.findByStatus(TicketStatus.CLOSED, pageable))
                .thenReturn(new PageImpl<>(List.of(ticketOwnedBy(1L))));

        ticketService.listAll(TicketStatus.CLOSED, pageable, admin);

        verify(ticketRepository).findByStatus(TicketStatus.CLOSED, pageable);
    }

    @Test
    void listAll_asUser_throwsForbiddenAndQueriesNothing() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> ticketService.listAll(null, pageable, user))
                .isInstanceOf(ForbiddenException.class);

        verify(ticketRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void get_someoneElsesTicket_throwsForbidden() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketOwnedBy(2L)));

        assertThatThrownBy(() -> ticketService.get(10L, user))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void get_asAdmin_returnsAnyTicket() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketOwnedBy(2L)));

        TicketResponse response = ticketService.get(10L, admin);

        assertThat(response.id()).isEqualTo(10L);
    }

    // ---- updateStatus (admin) ----

    @Test
    void updateStatus_asUser_throwsForbiddenAndSavesNothing() {
        assertThatThrownBy(() ->
                ticketService.updateStatus(10L, new UpdateStatusRequest(TicketStatus.IN_PROGRESS, null), user))
                .isInstanceOf(ForbiddenException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void updateStatus_startWork_changesStatusToInProgress() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketOwnedBy(1L)));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse response = ticketService.updateStatus(
                10L, new UpdateStatusRequest(TicketStatus.IN_PROGRESS, null), admin);

        assertThat(response.status()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void updateStatus_closeWithNote_savesNoteAndClosedAt() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketWithStatus(1L, TicketStatus.IN_PROGRESS)));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse response = ticketService.updateStatus(
                10L, new UpdateStatusRequest(TicketStatus.CLOSED, "  Restarted the router  "), admin);

        assertThat(response.status()).isEqualTo(TicketStatus.CLOSED);
        assertThat(response.resolutionNote()).isEqualTo("Restarted the router");
        assertThat(response.closedAt()).isNotNull();
    }

    @Test
    void updateStatus_closeWithoutNote_throwsBadRequestAndSavesNothing() {
        assertThatThrownBy(() ->
                ticketService.updateStatus(10L, new UpdateStatusRequest(TicketStatus.CLOSED, "   "), admin))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("resolution note");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void updateStatus_toOpen_throwsBadRequestAndSavesNothing() {
        assertThatThrownBy(() ->
                ticketService.updateStatus(10L, new UpdateStatusRequest(TicketStatus.OPEN, null), admin))
                .isInstanceOf(BadRequestException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void updateStatus_adminTryingToWithdraw_throwsBadRequestAndSavesNothing() {
        assertThatThrownBy(() ->
                ticketService.updateStatus(10L, new UpdateStatusRequest(TicketStatus.WITHDRAWN, null), admin))
                .isInstanceOf(BadRequestException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void updateStatus_missingTicket_throwsNotFound() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                ticketService.updateStatus(10L, new UpdateStatusRequest(TicketStatus.IN_PROGRESS, null), admin))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateStatus_fromClosed_throwsConflictAndSavesNothing() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketWithStatus(1L, TicketStatus.CLOSED)));

        assertThatThrownBy(() ->
                ticketService.updateStatus(10L, new UpdateStatusRequest(TicketStatus.IN_PROGRESS, null), admin))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CLOSED")
                .hasMessageContaining("IN_PROGRESS");

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void updateStatus_fromWithdrawn_throwsConflict() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketWithStatus(1L, TicketStatus.WITHDRAWN)));

        assertThatThrownBy(() ->
                ticketService.updateStatus(10L, new UpdateStatusRequest(TicketStatus.CLOSED, "Done"), admin))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateStatus_toSameStatus_throwsConflict() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketWithStatus(1L, TicketStatus.IN_PROGRESS)));

        assertThatThrownBy(() ->
                ticketService.updateStatus(10L, new UpdateStatusRequest(TicketStatus.IN_PROGRESS, null), admin))
                .isInstanceOf(ConflictException.class);
    }

    // ---- update (edit) ----

    @Test
    void update_byOwnerWhileOpen_changesFieldsAndKeepsStatus() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketOwnedBy(1L)));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse response = ticketService.update(
                10L, new UpdateTicketRequest("  New title ", " New text ", Priority.HIGH), user);

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.description()).isEqualTo("New text");
        assertThat(response.priority()).isEqualTo(Priority.HIGH);
        assertThat(response.status()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void update_whileInProgress_isAllowed() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketWithStatus(1L, TicketStatus.IN_PROGRESS)));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse response = ticketService.update(
                10L, new UpdateTicketRequest("Title", "Text", Priority.LOW), user);

        assertThat(response.status()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void update_byAnotherUser_throwsForbiddenAndSavesNothing() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketOwnedBy(2L)));

        assertThatThrownBy(() -> ticketService.update(
                10L, new UpdateTicketRequest("Title", "Text", Priority.LOW), user))
                .isInstanceOf(ForbiddenException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void update_byAdmin_canEditAnyOpenTicket() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketOwnedBy(2L)));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse response = ticketService.update(
                10L, new UpdateTicketRequest("By admin", "Text", Priority.MEDIUM), admin);

        assertThat(response.title()).isEqualTo("By admin");
    }

    @Test
    void update_whenClosed_throwsConflictAndSavesNothing() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketWithStatus(1L, TicketStatus.CLOSED)));

        assertThatThrownBy(() -> ticketService.update(
                10L, new UpdateTicketRequest("Title", "Text", Priority.LOW), user))
                .isInstanceOf(ConflictException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void update_whenWithdrawn_throwsConflict() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketWithStatus(1L, TicketStatus.WITHDRAWN)));

        assertThatThrownBy(() -> ticketService.update(
                10L, new UpdateTicketRequest("Title", "Text", Priority.LOW), user))
                .isInstanceOf(ConflictException.class);
    }

    // ---- withdraw (owner) ----

    @Test
    void withdraw_byOwnerWhileOpen_setsWithdrawnAndTimestamp() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketOwnedBy(1L)));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse response = ticketService.withdraw(10L, user);

        assertThat(response.status()).isEqualTo(TicketStatus.WITHDRAWN);
        assertThat(response.withdrawnAt()).isNotNull();
    }

    @Test
    void withdraw_byAnotherUser_throwsForbiddenAndSavesNothing() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketOwnedBy(2L)));

        assertThatThrownBy(() -> ticketService.withdraw(10L, user))
                .isInstanceOf(ForbiddenException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void withdraw_byAdminWhoIsNotTheOwner_throwsForbidden() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketOwnedBy(1L)));

        assertThatThrownBy(() -> ticketService.withdraw(10L, admin))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void withdraw_whenInProgress_throwsConflictAndSavesNothing() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketWithStatus(1L, TicketStatus.IN_PROGRESS)));

        assertThatThrownBy(() -> ticketService.withdraw(10L, user))
                .isInstanceOf(ConflictException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void withdraw_whenAlreadyWithdrawn_throwsConflict() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketWithStatus(1L, TicketStatus.WITHDRAWN)));

        assertThatThrownBy(() -> ticketService.withdraw(10L, user))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void withdraw_missingTicket_throwsNotFound() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.withdraw(10L, user))
                .isInstanceOf(NotFoundException.class);
    }

    // ---- history rows (Day 3) and idempotency (Day 5) ----

    @Test
    void create_writesOpenHistoryRow() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity(1L)));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        ticketService.create(new CreateTicketRequest("Wi-Fi", "down", Priority.HIGH), null, user);

        ArgumentCaptor<TicketStatusHistory> captor = ArgumentCaptor.forClass(TicketStatusHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getFromStatus()).isNull();
        assertThat(captor.getValue().getToStatus()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void updateStatus_startWork_writesHistoryRowWithAdmin() {
        User adminEntity = userEntity(99L);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticketOwnedBy(1L)));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.getReferenceById(99L)).thenReturn(adminEntity);

        ticketService.updateStatus(10L, new UpdateStatusRequest(TicketStatus.IN_PROGRESS, null), admin);

        ArgumentCaptor<TicketStatusHistory> captor = ArgumentCaptor.forClass(TicketStatusHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getFromStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(captor.getValue().getToStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(captor.getValue().getChangedBy()).isSameAs(adminEntity);
    }

    @Test
    void create_sameKeyDifferentBody_throwsBadRequestAndCreatesNothing() {
        when(idempotencyRepository.findByUserIdAndIdemKey(1L, "key-1"))
                .thenReturn(Optional.of(IdempotencyRecord.of(1L, "key-1", "hash-of-another-request", 10L)));

        assertThatThrownBy(() -> ticketService.create(
                new CreateTicketRequest("A", "B", Priority.LOW), "key-1", user))
                .isInstanceOf(BadRequestException.class);

        verify(ticketRepository, never()).save(any());
    }
}

