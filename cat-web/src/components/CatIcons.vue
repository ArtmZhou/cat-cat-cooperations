<script lang="ts">
// Functional SVG icon components for the dark tech cat theme.
// All icons use stroke style, 24x24 viewBox, stroke-width 1.5.
// Usage: <CatLogo :size="32" color="#7C3AED" />
import { defineComponent, h } from 'vue'

const iconProps = {
  size: { type: Number, default: 24 },
  color: { type: String, default: 'currentColor' }
}

function svgWrapper(size: number, children: any[]) {
  return h('svg', {
    xmlns: 'http://www.w3.org/2000/svg',
    width: size,
    height: size,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    'stroke-width': '1.5',
    'stroke-linecap': 'round',
    'stroke-linejoin': 'round'
  }, children)
}

export const CatLogo = defineComponent({
  name: 'CatLogo',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('path', { d: 'M12 22c-4.97 0-9-3.58-9-8 0-2.1.8-4.02 2.14-5.5L3 2l4.5 2.5C8.96 3.9 10.43 3.5 12 3.5s3.04.4 4.5 1L21 2l-2.14 6.5A8.48 8.48 0 0 1 21 14c0 4.42-4.03 8-9 8z' }),
      h('circle', { cx: '9', cy: '13', r: '1', fill: 'currentColor', stroke: 'none' }),
      h('circle', { cx: '15', cy: '13', r: '1', fill: 'currentColor', stroke: 'none' }),
      h('path', { d: 'M10 16s.5 1 2 1 2-1 2-1' })
    ])])
  }
})

export const RobotAgent = defineComponent({
  name: 'RobotAgent',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('rect', { x: '4', y: '6', width: '16', height: '12', rx: '3' }),
      h('circle', { cx: '9', cy: '12', r: '1.5', fill: 'currentColor', stroke: 'none' }),
      h('circle', { cx: '15', cy: '12', r: '1.5', fill: 'currentColor', stroke: 'none' }),
      h('path', { d: 'M10 15h4' }),
      h('path', { d: 'M12 2v4' }),
      h('path', { d: 'M2 10h2' }),
      h('path', { d: 'M20 10h2' })
    ])])
  }
})

export const GroupChatIcon = defineComponent({
  name: 'GroupChatIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('path', { d: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2' }),
      h('circle', { cx: '9', cy: '7', r: '4' }),
      h('path', { d: 'M22 21v-2a4 4 0 0 0-3-3.87' }),
      h('path', { d: 'M16 3.13a4 4 0 0 1 0 7.75' })
    ])])
  }
})

export const DashboardIcon = defineComponent({
  name: 'DashboardIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('rect', { x: '3', y: '3', width: '7', height: '9', rx: '1.5' }),
      h('rect', { x: '14', y: '3', width: '7', height: '5', rx: '1.5' }),
      h('rect', { x: '14', y: '12', width: '7', height: '9', rx: '1.5' }),
      h('rect', { x: '3', y: '16', width: '7', height: '5', rx: '1.5' })
    ])])
  }
})

export const TerminalIcon = defineComponent({
  name: 'TerminalIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('rect', { x: '2', y: '4', width: '20', height: '16', rx: '2' }),
      h('path', { d: 'M6 9l4 3-4 3' }),
      h('path', { d: 'M12 15h6' })
    ])])
  }
})

export const LightningIcon = defineComponent({
  name: 'LightningIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('path', { d: 'M13 2L3 14h9l-1 8 10-12h-9l1-8z', fill: 'currentColor', 'stroke-width': '0' })
    ])])
  }
})

export const ChartBarIcon = defineComponent({
  name: 'ChartBarIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('rect', { x: '3', y: '12', width: '4', height: '9', rx: '1' }),
      h('rect', { x: '10', y: '7', width: '4', height: '14', rx: '1' }),
      h('rect', { x: '17', y: '3', width: '4', height: '18', rx: '1' })
    ])])
  }
})

export const RefreshIcon = defineComponent({
  name: 'RefreshIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('path', { d: 'M21 2v6h-6' }),
      h('path', { d: 'M3 12a9 9 0 0 1 15-6.7L21 8' }),
      h('path', { d: 'M3 22v-6h6' }),
      h('path', { d: 'M21 12a9 9 0 0 1-15 6.7L3 16' })
    ])])
  }
})

export const PlusCircleIcon = defineComponent({
  name: 'PlusCircleIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('circle', { cx: '12', cy: '12', r: '10' }),
      h('path', { d: 'M12 8v8' }),
      h('path', { d: 'M8 12h8' })
    ])])
  }
})

export const MessageBubbleIcon = defineComponent({
  name: 'MessageBubbleIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('path', { d: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z' })
    ])])
  }
})

export const FoldIcon = defineComponent({
  name: 'FoldIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('path', { d: 'M3 6h18' }),
      h('path', { d: 'M3 12h12' }),
      h('path', { d: 'M3 18h18' }),
      h('path', { d: 'M18 9l3 3-3 3' })
    ])])
  }
})

export const ExpandIcon = defineComponent({
  name: 'ExpandIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('path', { d: 'M3 6h18' }),
      h('path', { d: 'M9 12h12' }),
      h('path', { d: 'M3 18h18' }),
      h('path', { d: 'M6 9l-3 3 3 3' })
    ])])
  }
})
</script>
