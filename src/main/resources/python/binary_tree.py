class Node:
    def __init__(self, text, depth, color=RED, text_color=BLUE, text_weight=NORMAL, font="Times New Roman", radius=0.6):
        self.circle = Circle(radius=radius, color=color)
        self.radius = radius
        self.text = Text(text, color=text_color)
        self.textValue = text
        self.left = None
        self.right = None
        self.rline = None
        self.lline = None
        self.text_weight = text_weight
        self.color = color
        self.font = font
        self.depth = depth
        self.width = 1.5
        self.text_color = text_color
        self.circle_text = VGroup(self.circle, self.text)
        if len(text) > 1:
            self.text.set_width(0.6 * self.circle.get_width())
        self.all = VGroup(self.circle_text)

    def set_left(self, node, scale):
        if self.left is not None:
            self.all.remove(self.left.all)

        self.left = node
        return self.set_left_mobject(node.circle, node.circle_text, scale)

    def set_left_mobject(self, shape, vgroup, scale):
        vgroup.next_to(self.circle_text, np.add(DOWN * 2 * scale, self.width * LEFT))

        if self.lline is None:
            line = Line(self.circle.point_at_angle(np.deg2rad(225)), shape.point_at_angle(np.deg2rad(90)),
                        color=GREEN)
            self.lline = line
            self.all.add(self.lline)
        else:
            self.lline.set_start_and_end_attrs(self.circle.point_at_angle(np.deg2rad(225)),
                                               shape.point_at_angle(np.deg2rad(90)))

        self.all.add(self.left.all)
        return [ShowCreation(self.lline)]

    def set_right(self, node, scale):
        if self.right is not None:
            self.all.remove(self.right.all)

        self.right = node
        return self.set_right_mobject(node.circle, node.circle_text, scale)

    def set_right_mobject(self, shape, vgroup, scale):
        vgroup.next_to(self.circle_text, np.add(DOWN * 2 * scale, self.width * RIGHT))
        if self.rline is None:
            line = Line(self.circle.point_at_angle(np.deg2rad(315)), shape.point_at_angle(np.deg2rad(90)),
                        color=GREEN)
            self.rline = line
            self.all.add(self.rline)
        else:
            self.rline.set_start_and_end_attrs(self.circle.point_at_angle(np.deg2rad(315)),
                                               shape.point_at_angle(np.deg2rad(90)))

        self.all.add(self.right.all)
        return [ShowCreation(self.rline)]

    def set_reference_right(self, tree):
        if self.right is not None:
            self.all.remove(self.right.all)

        # For pointer angle
        dummy = Node("", self.depth + 1, radius=0)
        rectangle = Rectangle_block(tree.identifier, color=self.color, text_color=self.text_color, font=self.font)
        self.right = rectangle
        animation = self.set_right_mobject(dummy.circle, dummy.circle_text)
        rectangle.all.move_to(dummy.all, UP)
        return animation

    def set_reference_left(self, tree, scale):
        if self.left is not None:
            self.all.remove(self.left.all)

        # For pointer angle
        dummy = Node("", self.depth + 1, radius=0)
        rectangle = Rectangle_block(tree.identifier, color=self.color, text_color=self.text_color, font=self.font)
        self.left = rectangle
        animation = self.set_left_mobject(dummy.circle, dummy.circle_text, scale)
        rectangle.all.move_to(dummy.all, UP)
        return animation

    def edit_node_value(self, text):
        new_text_obj = Text(text, color=self.text_color)
        animation = [ReplacementTransform(self.text, new_text_obj.move_to(self.circle_text.get_center()))]
        return animation

    def highlight(self):
        return [ApplyMethod(self.circle_text.set_color, YELLOW)]

    def unhighlight(self):
        return [ApplyMethod(self.circle.set_color, self.color), ApplyMethod(self.text.set_color, self.text_color)]

    # delete left assumes tree has left child
    def delete_left(self):
        if self.left is None:
            return []

        animation = [FadeOut(self.left.all), FadeOut(self.lline)]
        self.left = None
        self.lline = None
        return animation

    # delete right assumes tree has right child
    def delete_right(self):
        if self.right is None:
            return []

        animation = [FadeOut(self.right.all), FadeOut(self.rline)]
        self.right = None
        self.rline = None
        return animation


class Tree(DataStructure, ABC):
    def __init__(self, ul, ur, ll, lr, aligned_edge, root, identifier, color=RED, text_color=BLUE, text_weight=NORMAL,
                 font="Times New Roman", radius=0.6):
        self.margin = [0.2, 0.2, 0]
        super().__init__(np.add(ul, self.margin), np.subtract(ur, self.margin), np.add(ll, self.margin),
                         np.subtract(lr, self.margin), aligned_edge, color, text_color, text_weight, font)
        self.identifier = identifier
        self.radius = radius
        self.scale = 1
        self.root = root
        self.all.add(self.root.all)
        self.aligned_edge = np.average([ul, ur, ll, lr])

    def update_root(self, node):
        animations = self.root.delete_left() + self.root.delete_right() + [
            ReplacementTransform(self.root.circle_text, node.circle_text.move_to(self.root.circle.get_center())),
            FadeIn(node.all)]
        self.root = node
        return animations

    def check_overlapping_children(self, node):
        if node is None:
            return []

        if node.left is not None and node.right is not None:
            overlap_x = node.left.all.get_right()[0] - node.right.all.get_left()[0] + self.margin[0] * 2
            if overlap_x > 0:

                newRline = Line(node.circle.point_at_angle(np.deg2rad(315)),
                                np.add(node.right.circle.point_at_angle(np.deg2rad(90)), RIGHT * overlap_x / 2),
                                color=GREEN)
                newLline = Line(node.circle.point_at_angle(np.deg2rad(225)),
                                np.add(node.left.circle.point_at_angle(np.deg2rad(90)), LEFT * overlap_x / 2),
                                color=GREEN)

                animations = [
                    ApplyMethod(node.left.all.move_to, np.add(node.left.all.get_center(), LEFT * overlap_x / 2)),
                    Transform(node.lline, newLline),
                    ApplyMethod(node.right.all.move_to, np.add(node.right.all.get_center(), RIGHT * overlap_x / 2)),
                    Transform(node.rline, newRline),
                ]

                shrink, _ = self.shrink_if_cross_border(node.all, overlap_x / 2)
                if shrink:
                    animations.extend(shrink)

                return animations
            else:
                return []

        animations = self.check_overlapping_children(node.left)
        animations.extend(self.check_overlapping_children(node.right))
        return animations

    # Assumes parent is in the tree
    def set_right(self, parent, child):
        animations = parent.set_right(child, self.scale)

        overlapping_children = self.check_overlapping_children(self.root)

        if overlapping_children:
            animations.extend(overlapping_children)

        shrink, scale_factor = self.shrink_if_cross_border(parent.all)
        if shrink:
            animations.extend(shrink)

        target_radius = self.radius * (scale_factor if scale_factor else 1)
        self.scale = (scale_factor if scale_factor else 1)
        self.radius = target_radius
        # child.all.scale( target_radius / child.radius)
        return animations

    # Assumes parent is in the tree
    def set_left(self, parent, child):
        animations = parent.set_left(child, self.scale)

        overlapping_children = self.check_overlapping_children(self.root)

        if overlapping_children:
            animations.extend(overlapping_children)

        shrink, scale_factor = self.shrink_if_cross_border(parent.all)
        if shrink:
            animations.extend(shrink)

        target_radius = self.radius * (scale_factor if scale_factor else 1)
        self.scale = (scale_factor if scale_factor else 1)
        self.radius = target_radius
        # child.all.scale( target_radius / child.radius)
        return animations

    # Assumes parent is in the tree
    def delete_left(self, parent):
        return parent.delete_left()

    # Assumes parent is in the tree
    def delete_right(self, parent):
        return parent.delete_right()

    def edit_node_value(self, node, text):
        return node.edit_node_value(text)

    def highlight(self, node):
        return node.highlight()

    def unhighlight(self, node):
        return node.unhighlight()

    def create_init(self, _):
        name = Text(self.identifier)
        name.next_to(self.root.circle_text, UP, 0.3)
        self.all.add(name)
        return ApplyMethod(self.all.move_to, np.add(self.ul, self.ur) / 2 + DOWN)

    def set_reference_right(self, parent, tree):
        return parent.set_reference_right(tree)

    def set_reference_left(self, parent, tree):
        return parent.set_reference_left(tree, self.scale)

    def shrink_if_cross_border(self, node, offset_x=0):
        if node.get_left()[0] - offset_x < self.ll[0]:
            currRight = self.all.get_right()[0]
            currLeft = node.get_left()[0] - offset_x
            maxLeft = self.ll[0]
            targetWidth = currRight - maxLeft
            overflowWidth = currRight - currLeft
            scale = targetWidth / overflowWidth
            moveToTarget = self.all.get_center() + [self.ll[0] - currLeft, 0, 0]

            if node.get_bottom()[1] < self.ll[1]:
                currTop = self.all.get_top()[1]
                maxDown = self.ll[1]
                currBottom = node.get_bottom()[1]
                target_height = currTop - maxDown
                overflow_height = currTop - node.get_bottom()[1]
                scale = min(scale, target_height / overflow_height)
                moveToTarget += [0, self.ll[1] - currBottom, 0]

            return [ScaleInPlace(self.all, scale), ApplyMethod(self.all.move_to, moveToTarget)], scale
        elif node.get_right()[0] > self.lr[0]:
            currLeft = self.all.get_left()[0]
            currRight = node.get_right()[0] + offset_x
            maxRight = self.lr[0]
            targetWidth = maxRight - currLeft
            overflowWidth = currRight - currLeft
            scale = targetWidth / overflowWidth
            moveToTarget = self.all.get_center() + [self.lr[0] - currRight, 0, 0]

            if node.get_bottom()[1] < self.ll[1]:
                currTop = self.all.get_top()[1]
                maxDown = self.ll[1]
                currBottom = node.get_bottom()[1]
                target_height = currTop - maxDown
                overflow_height = currTop - node.get_bottom()[1]
                scale = min(scale, target_height / overflow_height)
                moveToTarget += [0, self.ll[1] - currBottom, 0]

            return [ScaleInPlace(self.all, scale), ApplyMethod(self.all.move_to, moveToTarget)], scale
        elif node.get_bottom()[1] < self.ll[1]:
            currTop = self.all.get_top()[1]
            maxDown = self.ll[1]
            currBottom = node.get_bottom()[1]
            target_height = currTop - maxDown
            overflow_height = currTop - node.get_bottom()[1]
            scale = target_height / overflow_height
            return [ScaleInPlace(self.all, scale),
                    ApplyMethod(self.all.move_to, self.all.get_center() + [0, self.ll[1] - currBottom, 0])], scale
        else:
            return 0, 1