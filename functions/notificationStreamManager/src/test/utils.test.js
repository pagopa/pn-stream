const { expect } = require("chai");
const {
  parseKinesisObjToJsonObj,
} = require("../app/lib/utils");

describe("test utils functions", () => {

  it("should parse kinesis obj", () => {
    const kinesisObj = {
      iun: { S: "abcd" },
      notificationAbstract: { S: "Sample abstract" },
      idempotenceToken: { S: "token123" },
      paNotificationId: { S: "paId123" },
      subject: { S: "Sample subject" },
      sentAt: { S: "2025-01-20T14:48:00.000Z" },
      cancelledIun: { S: "cancelledIun123" },
      cancelledByIun: { S: "cancelledByIun123" },
      senderPaId: { S: "026e8c72-7944-4dcd-8668-f596447fec6d" },
      recipients: {
        L: [
          {
            M: {
              recipientType: { S: "PF" },
              recipientId: { S: "recipient1" },
              denomination: { S: "Denomination1" },
              digitalDomicile: {
                M: {
                  type: { S: "PEC" },
                  address: { S: "recipient1@example.com" }
                }
              },
              physicalAddress: {
                M: {
                  at: { S: "At1" },
                  address: { S: "Address1" },
                  addressDetails: { S: "AddressDetails1" },
                  zip: { S: "Zip1" },
                  municipality: { S: "Municipality1" },
                  municipalityDetails: { S: "MunicipalityDetails1" },
                  province: { S: "Province1" },
                  foreignState: { S: "ForeignState1" }
                }
              },
              payments: {
                L: [
                  {
                    M: {
                      noticeCode: { S: "NoticeCode1" },
                      creditorTaxId: { S: "CreditorTaxId1" },
                      applyCost: { BOOL: true },
                      pagoPaForm: {
                        M: {
                          digests: {
                            M: {
                              sha256: { S: "sha256Digest1" }
                            }
                          },
                          contentType: { S: "application/pdf" },
                          ref: {
                            M: {
                              key: { S: "Key1" },
                              versionToken: { S: "VersionToken1" }
                            }
                          }
                        }
                      },
                      f24: {
                        M: {
                          title: { S: "Title1" },
                          applyCost: { BOOL: true },
                          metadataAttachment: {
                            M: {
                              digests: {
                                M: {
                                  sha256: { S: "sha256Digest2" }
                                }
                              },
                              contentType: { S: "application/pdf" },
                              ref: {
                                M: {
                                  key: { S: "Key2" },
                                  versionToken: { S: "VersionToken2" }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                ]
              }
            }
          }
        ]
      },
      documents: {
        L: [
          {
            M: {
              contentType: { S: "application/pdf" },
              digests: {
                M: {
                  sha256: { S: "sha256Digest5" }
                }
              },
              ref: {
                M: {
                  key: { S: "Key5" },
                  versionToken: { S: "VersionToken5" }
                }
              },
              title: { S: "Document1" },
              requireAck: { BOOL: true },
              sendByMail: { BOOL: false }
            }
          },
          {
            M: {
              contentType: { S: "application/pdf" },
              digests: {
                M: {
                  sha256: { S: "sha256Digest6" }
                }
              },
              ref: {
                M: {
                  key: { S: "Key6" },
                  versionToken: { S: "VersionToken6" }
                }
              },
              title: { S: "Document2" },
              requireAck: { BOOL: false },
              sendByMail: { BOOL: true }
            }
          }
        ]
      },
      notificationFeePolicy: { S: "FEE_POLICY" },
      physicalCommunicationType: { S: "PHYSICAL_COMMUNICATION_TYPE" },
      group: { S: "Group1" },
      senderDenomination: { S: "Sender Denomination" },
      senderTaxId: { S: "Sender Tax ID" },
      amount: { N: 100 },
      paymentExpirationDate: { S: "2025-12-31" },
      requestId: { S: "RequestId123" },
      taxonomyCode: { S: "TaxonomyCode123" },
      pagoPaIntMode: { S: "PagoPaIntMode" },
      sourceChannel: { S: "SourceChannel" },
      sourceChannelDetails: { S: "SourceChannelDetails" },
      version: { S: "v1" },
      paFee: { N: 10 },
      vat: { N: 20 }
    };
    const parsedObj = parseKinesisObjToJsonObj(kinesisObj);
    expect(parsedObj).to.eql({
      iun: "abcd",
      notificationAbstract: "Sample abstract",
      idempotenceToken: "token123",
      paNotificationId: "paId123",
      subject: "Sample subject",
      sentAt: "2025-01-20T14:48:00.000Z",
      cancelledIun: "cancelledIun123",
      cancelledByIun: "cancelledByIun123",
      senderPaId: "026e8c72-7944-4dcd-8668-f596447fec6d",
      recipients: [
        {
          recipientType: "PF",
          recipientId: "recipient1",
          denomination: "Denomination1",
          digitalDomicile: {
            type: "PEC",
            address: "recipient1@example.com"
          },
          physicalAddress: {
            at: "At1",
            address: "Address1",
            addressDetails: "AddressDetails1",
            zip: "Zip1",
            municipality: "Municipality1",
            municipalityDetails: "MunicipalityDetails1",
            province: "Province1",
            foreignState: "ForeignState1"
          },
          payments: [
            {
              noticeCode: "NoticeCode1",
              creditorTaxId: "CreditorTaxId1",
              applyCost: true,
              pagoPaForm: {
                digests: {
                  sha256: "sha256Digest1"
                },
                contentType: "application/pdf",
                ref: {
                  key: "Key1",
                  versionToken: "VersionToken1"
                }
              },
              f24: {
                title: "Title1",
                applyCost: true,
                metadataAttachment: {
                  digests: {
                    sha256: "sha256Digest2"
                  },
                  contentType: "application/pdf",
                  ref: {
                    key: "Key2",
                    versionToken: "VersionToken2"
                  }
                }
              }
            }
          ]
        }
      ],
      documents: [
        {
          contentType: "application/pdf",
          digests: {
            sha256: "sha256Digest5"
          },
          ref: {
            key: "Key5",
            versionToken: "VersionToken5"
          },
          title: "Document1",
          requireAck: true,
          sendByMail: false
        },
        {
          contentType: "application/pdf",
          digests: {
            sha256: "sha256Digest6"
          },
          ref: {
            key: "Key6",
            versionToken: "VersionToken6"
          },
          title: "Document2",
          requireAck: false,
          sendByMail: true
        }
      ],
      notificationFeePolicy: "FEE_POLICY",
      physicalCommunicationType: "PHYSICAL_COMMUNICATION_TYPE",
      group: "Group1",
      senderDenomination: "Sender Denomination",
      senderTaxId: "Sender Tax ID",
      amount: 100,
      paymentExpirationDate: "2025-12-31",
      requestId: "RequestId123",
      taxonomyCode: "TaxonomyCode123",
      pagoPaIntMode: "PagoPaIntMode",
      sourceChannel: "SourceChannel",
      sourceChannelDetails: "SourceChannelDetails",
      version: "v1",
      paFee: 10,
      vat: 20
    });
  });

  it("no kinesis obj", () => {
    const parsedObj = parseKinesisObjToJsonObj(null);
    expect(parsedObj).equal(null);
  });
});