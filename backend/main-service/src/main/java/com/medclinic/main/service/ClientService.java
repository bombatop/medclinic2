package com.medclinic.main.service;

import com.medclinic.main.dto.ClientResponse;
import com.medclinic.main.dto.CreateClientRequest;
import com.medclinic.main.dto.UpdateClientRequest;
import com.medclinic.main.exception.ResourceNotFoundException;
import com.medclinic.main.model.Client;
import com.medclinic.main.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    @Transactional
    public ClientResponse createClient(CreateClientRequest request) {
        Client client = Client.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .email(request.email())
                .notes(request.notes())
                .receiveAppointmentReminders(Boolean.TRUE.equals(request.receiveAppointmentReminders()))
                .telegramChatId(blankToNull(request.telegramChatId()))
                .build();

        return ClientResponse.from(clientRepository.save(client));
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> getAllClients() {
        return clientRepository.findAll().stream()
                .map(ClientResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ClientResponse> getAllClients(String search, Pageable pageable) {
        Specification<Client> spec = (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern),
                    cb.like(cb.lower(root.get("phone")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("email"), "")), pattern)
            );
        };
        return clientRepository.findAll(spec, pageable).map(ClientResponse::from);
    }

    @Transactional(readOnly = true)
    public ClientResponse getClientById(Long id) {
        return clientRepository.findById(id)
                .map(ClientResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
    }

    @Transactional
    public ClientResponse updateClient(Long id, UpdateClientRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (request.firstName() != null) {
            client.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            client.setLastName(request.lastName());
        }
        if (request.phone() != null) {
            client.setPhone(request.phone());
        }
        if (request.email() != null) {
            client.setEmail(request.email().isBlank() ? null : request.email());
        }
        if (request.notes() != null) {
            client.setNotes(request.notes().isBlank() ? null : request.notes());
        }
        if (request.receiveAppointmentReminders() != null) {
            client.setReceiveAppointmentReminders(request.receiveAppointmentReminders());
        }
        if (request.telegramChatId() != null) {
            client.setTelegramChatId(blankToNull(request.telegramChatId()));
        }

        return ClientResponse.from(clientRepository.save(client));
    }

    @Transactional
    public void deleteClient(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        clientRepository.delete(client);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
