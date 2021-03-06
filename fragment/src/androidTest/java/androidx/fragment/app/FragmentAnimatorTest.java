/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.fragment.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Build;
import android.os.Parcelable;
import android.util.Pair;
import android.view.View;

import androidx.annotation.AnimatorRes;
import androidx.annotation.RequiresApi;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.test.FragmentTestActivity;
import androidx.fragment.test.R;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class FragmentAnimatorTest {
    // These are pretend resource IDs for animators. We don't need real ones since we
    // load them by overriding onCreateAnimator
    @AnimatorRes
    private static final int ENTER = 1;
    @AnimatorRes
    private static final int EXIT = 2;
    @AnimatorRes
    private static final int POP_ENTER = 3;
    @AnimatorRes
    private static final int POP_EXIT = 4;

    @Rule
    public ActivityTestRule<FragmentTestActivity> mActivityRule =
            new ActivityTestRule<FragmentTestActivity>(FragmentTestActivity.class);

    @Before
    public void setupContainer() {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
    }

    // Ensure that adding and popping a Fragment uses the enter and popExit animators
    @Test
    public void addAnimators() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        // One fragment with a view
        final AnimatorFragment fragment = new AnimatorFragment();
        fm.beginTransaction()
                .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
                .add(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        assertEnterPopExit(fragment);
    }

    // Ensure that removing and popping a Fragment uses the exit and popEnter animators
    @Test
    public void removeAnimators() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        // One fragment with a view
        final AnimatorFragment fragment = new AnimatorFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment, "1")
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        fm.beginTransaction()
                .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
                .remove(fragment)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        assertExitPopEnter(fragment);
    }

    // Ensure that showing and popping a Fragment uses the enter and popExit animators
    // This tests reordered transactions
    @Test
    public void showAnimatorsReordered() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        // One fragment with a view
        final AnimatorFragment fragment = new AnimatorFragment();
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).hide(fragment).commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertEquals(View.GONE, fragment.requireView().getVisibility());

            }
        });

        fm.beginTransaction()
                .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
                .show(fragment)
                .addToBackStack(null)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertEquals(View.VISIBLE, fragment.requireView().getVisibility());
            }
        });

        assertEnterPopExit(fragment);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertEquals(View.GONE, fragment.requireView().getVisibility());
            }
        });
    }

    // Ensure that showing and popping a Fragment uses the enter and popExit animators
    // This tests ordered transactions
    @Test
    public void showAnimatorsOrdered() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        // One fragment with a view
        final AnimatorFragment fragment = new AnimatorFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment)
                .hide(fragment)
                .setReorderingAllowed(false)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertEquals(View.GONE, fragment.requireView().getVisibility());

            }
        });

        fm.beginTransaction()
                .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
                .show(fragment)
                .setReorderingAllowed(false)
                .addToBackStack(null)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertEquals(View.VISIBLE, fragment.requireView().getVisibility());
            }
        });

        assertEnterPopExit(fragment);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertEquals(View.GONE, fragment.requireView().getVisibility());
            }
        });
    }

    // Ensure that hiding and popping a Fragment uses the exit and popEnter animators
    @Test
    public void hideAnimators() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        // One fragment with a view
        final AnimatorFragment fragment = new AnimatorFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment, "1")
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        fm.beginTransaction()
                .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
                .hide(fragment)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        assertExitPopEnter(fragment);
    }

    // Ensure that attaching and popping a Fragment uses the enter and popExit animators
    @Test
    public void attachAnimators() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        // One fragment with a view
        final AnimatorFragment fragment = new AnimatorFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment)
                .detach(fragment)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        fm.beginTransaction()
                .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
                .attach(fragment)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        assertEnterPopExit(fragment);
    }

    // Ensure that detaching and popping a Fragment uses the exit and popEnter animators
    @Test
    public void detachAnimators() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        // One fragment with a view
        final AnimatorFragment fragment = new AnimatorFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment, "1")
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        fm.beginTransaction()
                .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
                .detach(fragment)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        assertExitPopEnter(fragment);
    }

    // Replace should exit the existing fragments and enter the added fragment, then
    // popping should popExit the removed fragment and popEnter the added fragments
    @Test
    public void replaceAnimators() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        // One fragment with a view
        final AnimatorFragment fragment1 = new AnimatorFragment();
        final AnimatorFragment fragment2 = new AnimatorFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1, "1")
                .add(R.id.fragmentContainer, fragment2, "2")
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        final AnimatorFragment fragment3 = new AnimatorFragment();
        fm.beginTransaction()
                .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
                .replace(R.id.fragmentContainer, fragment3)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        assertFragmentAnimation(fragment1, 1, false, EXIT);
        assertFragmentAnimation(fragment2, 1, false, EXIT);
        assertFragmentAnimation(fragment3, 1, true, ENTER);

        fm.popBackStack();
        FragmentTestUtil.waitForExecution(mActivityRule);

        assertFragmentAnimation(fragment3, 2, false, POP_EXIT);
        final AnimatorFragment replacement1 = (AnimatorFragment) fm.findFragmentByTag("1");
        final AnimatorFragment replacement2 = (AnimatorFragment) fm.findFragmentByTag("1");
        int expectedAnimations = replacement1 == fragment1 ? 2 : 1;
        assertFragmentAnimation(replacement1, expectedAnimations, true, POP_ENTER);
        assertFragmentAnimation(replacement2, expectedAnimations, true, POP_ENTER);
    }

    // Ensure that adding and popping a Fragment uses the enter and popExit animators,
    // but the animators are delayed when an entering Fragment is postponed.
    @Test
    public void postponedAddAnimators() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        final AnimatorFragment fragment = new AnimatorFragment();
        fragment.postponeEnterTransition();
        fm.beginTransaction()
                .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
                .add(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        assertPostponed(fragment, 0);
        fragment.startPostponedEnterTransition();

        FragmentTestUtil.waitForExecution(mActivityRule);
        assertEnterPopExit(fragment);
    }

    // Ensure that removing and popping a Fragment uses the exit and popEnter animators,
    // but the animators are delayed when an entering Fragment is postponed.
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    @Test
    public void postponedRemoveAnimators() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        final AnimatorFragment fragment = new AnimatorFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment, "1")
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        fm.beginTransaction()
                .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
                .remove(fragment)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        assertExitPostponedPopEnter(fragment);
    }

    // Ensure that adding and popping a Fragment is postponed in both directions
    // when the fragments have been marked for postponing.
    @Test
    public void postponedAddRemove() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        final AnimatorFragment fragment1 = new AnimatorFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        final AnimatorFragment fragment2 = new AnimatorFragment();
        fragment2.postponeEnterTransition();

        fm.beginTransaction()
                .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();

        FragmentTestUtil.waitForExecution(mActivityRule);

        assertPostponed(fragment2, 0);
        assertNotNull(fragment1.getView());
        assertEquals(View.VISIBLE, fragment1.getView().getVisibility());
        assertEquals(1f, fragment1.getView().getAlpha(), 0f);
        assertTrue(ViewCompat.isAttachedToWindow(fragment1.getView()));

        fragment2.startPostponedEnterTransition();
        FragmentTestUtil.waitForExecution(mActivityRule);

        assertExitPostponedPopEnter(fragment1);
    }

    // Popping a postponed transaction should result in no animators
    @Test
    public void popPostponed() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        final AnimatorFragment fragment1 = new AnimatorFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);
        assertEquals(0, fragment1.numAnimators);

        final AnimatorFragment fragment2 = new AnimatorFragment();
        fragment2.postponeEnterTransition();

        fm.beginTransaction()
                .setCustomAnimations(ENTER, EXIT, POP_ENTER, POP_EXIT)
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();

        FragmentTestUtil.waitForExecution(mActivityRule);

        assertPostponed(fragment2, 0);

        // Now pop the postponed transaction
        FragmentTestUtil.popBackStackImmediate(mActivityRule);

        assertNotNull(fragment1.getView());
        assertEquals(1f, fragment1.getView().getAlpha(), 0f);
        assertTrue(ViewCompat.isAttachedToWindow(fragment1.getView()));
        assertTrue(fragment1.isAdded());

        assertNull(fragment2.getView());
        assertFalse(fragment2.isAdded());

        assertEquals(0, fragment1.numAnimators);
        assertEquals(0, fragment2.numAnimators);
        assertNull(fragment1.animator);
        assertNull(fragment2.animator);
    }

    // Make sure that if the state was saved while a Fragment was animating that its
    // state is proper after restoring.
    @Test
    public void saveWhileAnimatingAway() throws Throwable {
        final FragmentController fc1 = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc1, null);

        final FragmentManager fm1 = fc1.getSupportFragmentManager();

        StrictViewFragment fragment1 = new StrictViewFragment();
        fragment1.setLayoutId(R.layout.scene1);
        fm1.beginTransaction()
                .add(R.id.fragmentContainer, fragment1, "1")
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        StrictViewFragment fragment2 = new StrictViewFragment();

        fm1.beginTransaction()
                .setCustomAnimations(0, 0, 0, R.animator.slow_fade_out)
                .replace(R.id.fragmentContainer, fragment2, "2")
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule, fm1);
        FragmentTestUtil.waitForExecution(mActivityRule);

        fm1.popBackStack();

        FragmentTestUtil.executePendingTransactions(mActivityRule, fm1);
        FragmentTestUtil.waitForExecution(mActivityRule);
        // Now fragment2 should be animating away
        assertFalse(fragment2.isAdded());
        assertEquals(fragment2, fm1.findFragmentByTag("2")); // still exists because it is animating

        Pair<Parcelable, FragmentManagerNonConfig> state =
                FragmentTestUtil.destroy(mActivityRule, fc1);

        final FragmentController fc2 = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc2, state);

        final FragmentManager fm2 = fc2.getSupportFragmentManager();
        Fragment fragment2restored = fm2.findFragmentByTag("2");
        assertNull(fragment2restored);

        Fragment fragment1restored = fm2.findFragmentByTag("1");
        assertNotNull(fragment1restored);
        assertNotNull(fragment1restored.getView());
    }

    private void assertEnterPopExit(AnimatorFragment fragment) throws Throwable {
        assertFragmentAnimation(fragment, 1, true, ENTER);

        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        fm.popBackStack();
        FragmentTestUtil.waitForExecution(mActivityRule);

        assertFragmentAnimation(fragment, 2, false, POP_EXIT);
    }

    private void assertExitPopEnter(AnimatorFragment fragment) throws Throwable {
        assertFragmentAnimation(fragment, 1, false, EXIT);

        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        fm.popBackStack();
        FragmentTestUtil.waitForExecution(mActivityRule);

        AnimatorFragment replacement = (AnimatorFragment) fm.findFragmentByTag("1");

        boolean isSameFragment = replacement == fragment;
        int expectedAnimators = isSameFragment ? 2 : 1;
        assertFragmentAnimation(replacement, expectedAnimators, true, POP_ENTER);
    }

    private void assertExitPostponedPopEnter(AnimatorFragment fragment) throws Throwable {
        assertFragmentAnimation(fragment, 1, false, EXIT);

        fragment.postponeEnterTransition();
        FragmentTestUtil.popBackStackImmediate(mActivityRule);

        assertPostponed(fragment, 1);

        fragment.startPostponedEnterTransition();
        FragmentTestUtil.waitForExecution(mActivityRule);
        assertFragmentAnimation(fragment, 2, true, POP_ENTER);
    }

    private void assertFragmentAnimation(AnimatorFragment fragment, int numAnimators,
            boolean isEnter, int animatorResourceId) throws InterruptedException {
        assertEquals(numAnimators, fragment.numAnimators);
        assertEquals(isEnter, fragment.enter);
        assertEquals(animatorResourceId, fragment.resourceId);
        assertNotNull(fragment.animator);
        assertTrue(fragment.wasStarted);
        assertTrue(fragment.endLatch.await(200, TimeUnit.MILLISECONDS));
    }

    private void assertPostponed(AnimatorFragment fragment, int expectedAnimators)
            throws InterruptedException {
        assertTrue(fragment.mOnCreateViewCalled);
        assertEquals(View.VISIBLE, fragment.requireView().getVisibility());
        assertEquals(0f, fragment.requireView().getAlpha(), 0f);
        assertEquals(expectedAnimators, fragment.numAnimators);
    }

    public static class AnimatorFragment extends StrictViewFragment {
        public int numAnimators;
        public Animator animator;
        public boolean enter;
        public int resourceId;
        public boolean wasStarted;
        public CountDownLatch endLatch;

        @Override
        public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
            if (nextAnim == 0) {
                return null;
            }
            this.numAnimators++;
            this.wasStarted = false;
            this.animator = ValueAnimator.ofFloat(0, 1).setDuration(1);
            this.endLatch = new CountDownLatch(1);
            this.animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    wasStarted = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    endLatch.countDown();
                }
            });
            this.resourceId = nextAnim;
            this.enter = enter;
            return this.animator;
        }
    }
}
