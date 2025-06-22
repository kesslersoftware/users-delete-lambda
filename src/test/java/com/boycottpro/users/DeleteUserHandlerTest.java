package com.boycottpro.users;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeleteUserHandlerTest {

    @Mock
    private DynamoDbClient dynamoDb;

    @Mock
    private Context context;

    @InjectMocks
    private DeleteUserHandler handler;

    @Test
    public void testDeleteUser_Successful() {
        String testUserId = "user123";

        // Set up mock request event with path parameter
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPathParameters(Map.of("user_id", testUserId));

        // Stub deleteItem call
        when(dynamoDb.deleteItem(any(DeleteItemRequest.class)))
                .thenReturn(DeleteItemResponse.builder().build());

        var response = handler.handleRequest(request, context);

        // Assertions
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("user deleted successfully"));

        // Verify deleteItem called with correct request
        ArgumentCaptor<DeleteItemRequest> captor = ArgumentCaptor.forClass(DeleteItemRequest.class);
        verify(dynamoDb).deleteItem(captor.capture());
        DeleteItemRequest capturedRequest = captor.getValue();

        assertEquals("users", capturedRequest.tableName());
        assertEquals(testUserId, capturedRequest.key().get("user_id").s());
    }

    @Test
    public void testDeleteUser_MissingUserId() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPathParameters(Map.of()); // No user_id provided

        var response = handler.handleRequest(request, context);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Missing user_id"));
        verify(dynamoDb, never()).deleteItem((DeleteItemRequest) any());
    }

    @Test
    public void testDeleteUser_DynamoDbException() {
        String testUserId = "user456";
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPathParameters(Map.of("user_id", testUserId));

        when(dynamoDb.deleteItem(any(DeleteItemRequest.class)))
                .thenThrow(new RuntimeException("DynamoDB error"));

        var response = handler.handleRequest(request, context);

        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("Unexpected server error"));
    }
}
