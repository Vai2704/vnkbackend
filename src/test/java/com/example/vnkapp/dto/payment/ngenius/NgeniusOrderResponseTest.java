package com.example.vnkapp.dto.payment.ngenius;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NgeniusOrderResponseTest {

  private static final String SAMPLE_RESPONSE = """
      {
          "_id": "urn:order:0f95e884-bb88-4019-b37a-64d70cae19d0",
          "_links": {
              "payment": {
                  "href": "https://paypage.sandbox.ngenius-payments.com/?code=deca45598c8019e8"
              }
          },
          "reference": "ORD-20260814-123456",
          "_embedded": {
              "payment": [
                  {
                      "_id": "urn:payment:ca31277e-2517-4564-bd50-f018fcc88bd1",
                      "reference": "ca31277e-2517-4564-bd50-f018fcc88bd1",
                      "state": "STARTED"
                  }
              ]
          }
      }
      """;

  @Test
  void deserializesCreateOrderResponse() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    NgeniusOrderResponse response = mapper.readValue(SAMPLE_RESPONSE, NgeniusOrderResponse.class);

    assertEquals("urn:order:0f95e884-bb88-4019-b37a-64d70cae19d0", response.id());
    assertEquals("ORD-20260814-123456", response.reference());
    assertEquals("https://paypage.sandbox.ngenius-payments.com/?code=deca45598c8019e8",
        response.paymentUrl());
    assertEquals("ca31277e-2517-4564-bd50-f018fcc88bd1", response.paymentId());
    assertNotNull(response.embedded());
  }
}
