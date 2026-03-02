package com.medclinic.main.service;

import com.medclinic.main.dto.ClientResponse;
import com.medclinic.main.dto.CreateClientRequest;
import com.medclinic.main.dto.UpdateClientRequest;
import com.medclinic.main.exception.ResourceNotFoundException;
import com.medclinic.main.model.Client;
import com.medclinic.main.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
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
            client.setEmail(request.email());
        }
        if (request.notes() != null) {
            client.setNotes(request.notes());
        }

        return ClientResponse.from(clientRepository.save(client));
    }
}
