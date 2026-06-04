package dev.monkeypatch.rctiming.forwarder.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.73.0)",
    comments = "Source: timing.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class TimingServiceGrpc {

  private TimingServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "dev.monkeypatch.timing.TimingService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<dev.monkeypatch.rctiming.forwarder.proto.LapPassing,
      dev.monkeypatch.rctiming.forwarder.proto.ForwarderCommand> getStreamPassingsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StreamPassings",
      requestType = dev.monkeypatch.rctiming.forwarder.proto.LapPassing.class,
      responseType = dev.monkeypatch.rctiming.forwarder.proto.ForwarderCommand.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<dev.monkeypatch.rctiming.forwarder.proto.LapPassing,
      dev.monkeypatch.rctiming.forwarder.proto.ForwarderCommand> getStreamPassingsMethod() {
    io.grpc.MethodDescriptor<dev.monkeypatch.rctiming.forwarder.proto.LapPassing, dev.monkeypatch.rctiming.forwarder.proto.ForwarderCommand> getStreamPassingsMethod;
    if ((getStreamPassingsMethod = TimingServiceGrpc.getStreamPassingsMethod) == null) {
      synchronized (TimingServiceGrpc.class) {
        if ((getStreamPassingsMethod = TimingServiceGrpc.getStreamPassingsMethod) == null) {
          TimingServiceGrpc.getStreamPassingsMethod = getStreamPassingsMethod =
              io.grpc.MethodDescriptor.<dev.monkeypatch.rctiming.forwarder.proto.LapPassing, dev.monkeypatch.rctiming.forwarder.proto.ForwarderCommand>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StreamPassings"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  dev.monkeypatch.rctiming.forwarder.proto.LapPassing.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  dev.monkeypatch.rctiming.forwarder.proto.ForwarderCommand.getDefaultInstance()))
              .setSchemaDescriptor(new TimingServiceMethodDescriptorSupplier("StreamPassings"))
              .build();
        }
      }
    }
    return getStreamPassingsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<dev.monkeypatch.rctiming.forwarder.proto.ForwarderStatus,
      dev.monkeypatch.rctiming.forwarder.proto.StatusAck> getReportStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReportStatus",
      requestType = dev.monkeypatch.rctiming.forwarder.proto.ForwarderStatus.class,
      responseType = dev.monkeypatch.rctiming.forwarder.proto.StatusAck.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<dev.monkeypatch.rctiming.forwarder.proto.ForwarderStatus,
      dev.monkeypatch.rctiming.forwarder.proto.StatusAck> getReportStatusMethod() {
    io.grpc.MethodDescriptor<dev.monkeypatch.rctiming.forwarder.proto.ForwarderStatus, dev.monkeypatch.rctiming.forwarder.proto.StatusAck> getReportStatusMethod;
    if ((getReportStatusMethod = TimingServiceGrpc.getReportStatusMethod) == null) {
      synchronized (TimingServiceGrpc.class) {
        if ((getReportStatusMethod = TimingServiceGrpc.getReportStatusMethod) == null) {
          TimingServiceGrpc.getReportStatusMethod = getReportStatusMethod =
              io.grpc.MethodDescriptor.<dev.monkeypatch.rctiming.forwarder.proto.ForwarderStatus, dev.monkeypatch.rctiming.forwarder.proto.StatusAck>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReportStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  dev.monkeypatch.rctiming.forwarder.proto.ForwarderStatus.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  dev.monkeypatch.rctiming.forwarder.proto.StatusAck.getDefaultInstance()))
              .setSchemaDescriptor(new TimingServiceMethodDescriptorSupplier("ReportStatus"))
              .build();
        }
      }
    }
    return getReportStatusMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static TimingServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TimingServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TimingServiceStub>() {
        @java.lang.Override
        public TimingServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TimingServiceStub(channel, callOptions);
        }
      };
    return TimingServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static TimingServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TimingServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TimingServiceBlockingV2Stub>() {
        @java.lang.Override
        public TimingServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TimingServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return TimingServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static TimingServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TimingServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TimingServiceBlockingStub>() {
        @java.lang.Override
        public TimingServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TimingServiceBlockingStub(channel, callOptions);
        }
      };
    return TimingServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static TimingServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TimingServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TimingServiceFutureStub>() {
        @java.lang.Override
        public TimingServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TimingServiceFutureStub(channel, callOptions);
        }
      };
    return TimingServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default io.grpc.stub.StreamObserver<dev.monkeypatch.rctiming.forwarder.proto.LapPassing> streamPassings(
        io.grpc.stub.StreamObserver<dev.monkeypatch.rctiming.forwarder.proto.ForwarderCommand> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getStreamPassingsMethod(), responseObserver);
    }

    /**
     */
    default void reportStatus(dev.monkeypatch.rctiming.forwarder.proto.ForwarderStatus request,
        io.grpc.stub.StreamObserver<dev.monkeypatch.rctiming.forwarder.proto.StatusAck> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getReportStatusMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service TimingService.
   */
  public static abstract class TimingServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return TimingServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service TimingService.
   */
  public static final class TimingServiceStub
      extends io.grpc.stub.AbstractAsyncStub<TimingServiceStub> {
    private TimingServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TimingServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TimingServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<dev.monkeypatch.rctiming.forwarder.proto.LapPassing> streamPassings(
        io.grpc.stub.StreamObserver<dev.monkeypatch.rctiming.forwarder.proto.ForwarderCommand> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getStreamPassingsMethod(), getCallOptions()), responseObserver);
    }

    /**
     */
    public void reportStatus(dev.monkeypatch.rctiming.forwarder.proto.ForwarderStatus request,
        io.grpc.stub.StreamObserver<dev.monkeypatch.rctiming.forwarder.proto.StatusAck> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getReportStatusMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service TimingService.
   */
  public static final class TimingServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<TimingServiceBlockingV2Stub> {
    private TimingServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TimingServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TimingServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<dev.monkeypatch.rctiming.forwarder.proto.LapPassing, dev.monkeypatch.rctiming.forwarder.proto.ForwarderCommand>
        streamPassings() {
      return io.grpc.stub.ClientCalls.blockingBidiStreamingCall(
          getChannel(), getStreamPassingsMethod(), getCallOptions());
    }

    /**
     */
    public dev.monkeypatch.rctiming.forwarder.proto.StatusAck reportStatus(dev.monkeypatch.rctiming.forwarder.proto.ForwarderStatus request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReportStatusMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service TimingService.
   */
  public static final class TimingServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<TimingServiceBlockingStub> {
    private TimingServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TimingServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TimingServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public dev.monkeypatch.rctiming.forwarder.proto.StatusAck reportStatus(dev.monkeypatch.rctiming.forwarder.proto.ForwarderStatus request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReportStatusMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service TimingService.
   */
  public static final class TimingServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<TimingServiceFutureStub> {
    private TimingServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TimingServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TimingServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<dev.monkeypatch.rctiming.forwarder.proto.StatusAck> reportStatus(
        dev.monkeypatch.rctiming.forwarder.proto.ForwarderStatus request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getReportStatusMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_REPORT_STATUS = 0;
  private static final int METHODID_STREAM_PASSINGS = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_REPORT_STATUS:
          serviceImpl.reportStatus((dev.monkeypatch.rctiming.forwarder.proto.ForwarderStatus) request,
              (io.grpc.stub.StreamObserver<dev.monkeypatch.rctiming.forwarder.proto.StatusAck>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_STREAM_PASSINGS:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.streamPassings(
              (io.grpc.stub.StreamObserver<dev.monkeypatch.rctiming.forwarder.proto.ForwarderCommand>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getStreamPassingsMethod(),
          io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
            new MethodHandlers<
              dev.monkeypatch.rctiming.forwarder.proto.LapPassing,
              dev.monkeypatch.rctiming.forwarder.proto.ForwarderCommand>(
                service, METHODID_STREAM_PASSINGS)))
        .addMethod(
          getReportStatusMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              dev.monkeypatch.rctiming.forwarder.proto.ForwarderStatus,
              dev.monkeypatch.rctiming.forwarder.proto.StatusAck>(
                service, METHODID_REPORT_STATUS)))
        .build();
  }

  private static abstract class TimingServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    TimingServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return dev.monkeypatch.rctiming.forwarder.proto.Timing.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("TimingService");
    }
  }

  private static final class TimingServiceFileDescriptorSupplier
      extends TimingServiceBaseDescriptorSupplier {
    TimingServiceFileDescriptorSupplier() {}
  }

  private static final class TimingServiceMethodDescriptorSupplier
      extends TimingServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    TimingServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (TimingServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new TimingServiceFileDescriptorSupplier())
              .addMethod(getStreamPassingsMethod())
              .addMethod(getReportStatusMethod())
              .build();
        }
      }
    }
    return result;
  }
}
