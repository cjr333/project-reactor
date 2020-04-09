import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.MockingDetails;
import org.mockito.internal.MockitoCore;
import org.mockito.internal.invocation.InvocationMatcher;
import org.mockito.internal.listeners.VerificationStartedNotifier;
import org.mockito.internal.matchers.Any;
import org.mockito.internal.progress.MockingProgress;
import org.mockito.internal.verification.MockAwareVerificationMode;
import org.mockito.internal.verification.api.VerificationData;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.MatchableInvocation;
import org.mockito.invocation.MockHandler;
import org.mockito.listeners.VerificationListener;
import org.mockito.verification.VerificationMode;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.exceptions.Reporter.notAMockPassedToVerify;
import static org.mockito.internal.exceptions.Reporter.nullPassedToVerify;
import static org.mockito.internal.exceptions.Reporter.stubPassedToVerify;
import static org.mockito.internal.progress.ThreadSafeMockingProgress.mockingProgress;
import static org.mockito.internal.util.MockUtil.getMockHandler;

public class LibraryTest {
  public static class Target {
    Integer num;

    public void setNum(Integer num) {
      this.num = num;
    }

    public void setNum2(Integer num, String a) {
      this.num = num;
      System.out.println(a);
    }
  }

  @Test
  public void verifySample() {
    Target mock = mock(Target.class);
    mock.setNum(1);
    mock.setNum(1);
    mock.setNum(2);
    verify(mock, times(2)).setNum(1);

    mock.setNum2(1, "a");
    mock.setNum2(1, "b");
    verify(mock, times(1)).setNum2(1, "b");

    verify(mock, times(2)).setNum2(any(), any());
  }

  @Test
  public void verifySample2() {
    Target mock = mock(Target.class);
    mock.setNum(1);
    mock.setNum(1);
    //    mock.setNum(2);
    new MockitoCoreExt().verify(mock, times(2)).setNum(1);

    mock.setNum2(1, "b");
    new MockitoCoreExt().verify(mock, times(1)).setNum2(1, "b");
  }

  @Test
  public void verifySample3() {
    Target mock = mock(Target.class);
    mock.setNum(1);
    verify(mock, times(1)).setNum(1);

    mock.setNum2(1, "b");
    verify(mock, times(1)).setNum2(2, "b");
  }

  class MockitoCoreExt extends MockitoCore {
    @SuppressWarnings("unchecked")
    @Override
    public <T> T verify(T mock, VerificationMode mode) {
      if (mock == null) {
        throw nullPassedToVerify();
      }
      MockingDetails mockingDetails = mockingDetails(mock);
      if (!mockingDetails.isMock()) {
        throw notAMockPassedToVerify(mock.getClass());
      }
      assertNotStubOnlyMock(mock);
      MockHandler handler = mockingDetails.getMockHandler();
      mock = (T) VerificationStartedNotifier.notifyVerificationStarted(
          handler.getMockSettings().getVerificationStartedListeners(), mockingDetails);

      MockingProgress mockingProgress = mockingProgress();
      VerificationMode actualMode = mockingProgress.maybeVerifyLazily(mode);
      mockingProgress.verificationStarted(new MockAwareVerificationModeExt(mock, actualMode, mockingProgress.verificationListeners()));
      return mock;
    }

    private void assertNotStubOnlyMock(Object mock) {
      if (getMockHandler(mock).getMockSettings().isStubOnly()) {
        throw stubPassedToVerify(mock);
      }
    }
  }

  class MockAwareVerificationModeExt extends MockAwareVerificationMode {
    public MockAwareVerificationModeExt(Object mock, VerificationMode mode, Set<VerificationListener> listeners) {
      super(mock, mode, listeners);
    }

    @Override
    public void verify(VerificationData data) {
      super.verify(data);

      // additional verify for any args
      MatchableInvocation target = data.getTarget();
      List<ArgumentMatcher> anyMatchers = IntStream.range(0, target.getMatchers().size()).mapToObj(i -> Any.ANY).collect(Collectors.toList());
      InvocationMatcher anyUnvocationMatcher = new InvocationMatcher(target.getInvocation(), anyMatchers);
      VerificationData anyVerificationData = new VerificationData() {
        @Override
        public List<Invocation> getAllInvocations() {
          return data.getAllInvocations();
        }

        @Override
        public MatchableInvocation getTarget() {
          return anyUnvocationMatcher;
        }

        @Override
        public InvocationMatcher getWanted() {
          return anyUnvocationMatcher;
        }
      };

      super.verify(anyVerificationData);
    }
  }
}
