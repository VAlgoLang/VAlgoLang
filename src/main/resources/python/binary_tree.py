class Node:
    def __init__(self, text, color=RED, text_color=BLUE, line_color=GREEN, highlight_colour=YELLOW, text_weight=NORMAL, font="Times New Roman", radius=0.6):
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
        self.line_color = line_color
        self.highlight_colour = highlight_colour
        self.font = font
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
        animations = []
        if self.lline is None:
            line = Line(self.circle.point_at_angle(np.deg2rad(225)), shape.point_at_angle(np.deg2rad(90)),
                        color=self.line_color)
            self.lline = line
            self.all.add(self.lline)
            animations.append(ShowCreation(self.lline))
        else:
            self.lline.set_start_and_end_attrs(self.circle.point_at_angle(np.deg2rad(225)),
                                               shape.point_at_angle(np.deg2rad(90)))

        self.all.add(self.left.all)
        return animations

    def set_right(self, node, scale):
        if self.right is not None:
            self.all.remove(self.right.all)

        self.right = node
        return self.set_right_mobject(node.circle, node.circle_text, scale)

    def set_right_mobject(self, shape, vgroup, scale):
        vgroup.next_to(self.circle_text, np.add(DOWN * 2 * scale, self.width * RIGHT))
        animations = []
        if self.rline is None:
            line = Line(self.circle.point_at_angle(np.deg2rad(315)), shape.point_at_angle(np.deg2rad(90)),
                        color=self.line_color)
            self.rline = line
            self.all.add(self.rline)
            animations.append(ShowCreation(self.rline))
        else:
            self.rline.set_start_and_end_attrs(self.circle.point_at_angle(np.deg2rad(315)),
                                               shape.point_at_angle(np.deg2rad(90)))

        self.all.add(self.right.all)
        return animations

    def set_reference_right(self, tree):
        if self.right is not None:
            self.all.remove(self.right.all)

        # For pointer angle
        dummy = Node("", radius=0)
        rectangle = Rectangle_block(tree.identifier, color=self.color, text_color=self.text_color, font=self.font)
        self.right = rectangle
        animation = self.set_right_mobject(dummy.circle, dummy.circle_text)
        rectangle.all.move_to(dummy.all, UP)
        return animation

    def set_reference_left(self, tree, scale):
        if self.left is not None:
            self.all.remove(self.left.all)

        # For pointer angle
        dummy = Node("", radius=0)
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
        return [ApplyMethod(self.circle_text.set_color, self.highlight_colour)]

    def unhighlight(self):
        return [ApplyMethod(self.circle.set_color, self.color), ApplyMethod(self.text.set_color, self.text_color)]

    # delete left assumes tree has left child
    def delete_left(self):
        if self.left is None:
            return []

        animation = [FadeOut(self.left.all)]
        self.all.remove(self.left.all, self.lline)
        self.left = None
        self.lline = None
        return animation

    # delete right assumes tree has right child
    def delete_right(self):
        if self.right is None:
            return []

        animation = [FadeOut(self.right.all)]
        self.all.remove(self.right.all, self.rline)
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
        self.max_radius = radius * 1.3
        self.scale = 1
        self.root = root
        self.all.add(self.root.all)
        self.aligned_edge = np.average([ul, ur, ll, lr])

    def create_init(self):
        name = Text(self.identifier)
        textPadding = [0, 0.3, 0]
        name.next_to(self.root.circle_text, UP, textPadding[1])
        self.all.add(name)
        return ApplyMethod(self.all.move_to, self.aligned_edge)

    def update_root(self, node):
        animations = self.root.delete_left() + self.root.delete_right() + [
            ReplacementTransform(self.root.circle_text, node.circle_text.move_to(self.root.circle.get_center())),
            FadeIn(node.all)]
        self.root = node
        return animations

    # Assumes parent is in the tree
    def set_right(self, parent, child):
        animations = parent.set_right(child, self.scale)
        return self._resize_after_modification(parent, animations)

    # Assumes parent is in the tree
    def set_left(self, parent, child):
        animations = parent.set_left(child, self.scale)
        return self._resize_after_modification(parent, animations)

    # Assumes parent is in the tree
    def delete_left(self, parent):
        animations = parent.delete_left()
        return self._resize_after_modification(parent, animations)

    # Assumes parent is in the tree
    def delete_right(self, parent):
        animations = parent.delete_right()
        return self._resize_after_modification(parent, animations)

    def edit_node_value(self, node, text):
        return node.edit_node_value(text)

    def set_reference_right(self, parent, tree):
        return parent.set_reference_right(tree)

    def set_reference_left(self, parent, tree):
        return parent.set_reference_left(tree, self.scale)

    def _resize_after_modification(self, parent, animations):
        scale_animations, scale_factor = self.check_positioning(parent)
        animations.extend(scale_animations)

        target_radius = self.radius * (scale_factor if scale_factor else 1)
        self.scale = (scale_factor if scale_factor else 1)
        self.radius = target_radius
        return animations

    # Assumes node is in the tree
    def check_positioning(self, node):
        animations = []
        overlapping_children, scale = self.check_overlapping_children(self.root)

        if len(overlapping_children) > 0:
            animations.extend(overlapping_children)
        else:
            shrink, scale_factor = self.shrink_if_cross_border(node.all)
            scale = min(scale, scale_factor)
            if shrink:
                animations.extend(shrink)
            else:
                grow, scale = self.grow_if_small()
                if grow:
                    animations.extend(grow)

        return animations, scale

     # Assumes node is in the tree
    def crossing_bottom_border(self, node):
        currTop = self.all.get_top()[1]
        maxDown = self.ll[1]
        currBottom = node.get_bottom()[1]
        target_height = currTop - maxDown
        overflow_height = currTop - currBottom
        scale = target_height / overflow_height
        return scale


    # Assumes node is in the tree
    def crossing_left_right_border(self, node, offset_x, left, scale=10e9):
        if left:
            currRight = self.all.get_right()[0]
            currLeft = node.get_left()[0]
        else:
            currLeft = self.all.get_left()[0]
            currRight = node.get_right()[0]

        targetWidth = abs(self.lr[0] - self.ll[0])
        overflowWidth = currRight - currLeft + 2 * offset_x
        scale = min(scale, targetWidth / overflowWidth)
        return scale

    def grow_if_small(self):
        targetWidth = abs(self.lr[0] - self.ll[0])
        targetHeight = abs(self.lr[1] - self.ul[1])
        currWidth = self.all.get_width()
        currHeight = self.all.get_height() + 0.4 #label offset
        scale = min(targetWidth / currWidth, targetHeight / currHeight)
        if scale >= 1:
            # check radius size
            targetRadius = self.radius * scale
            if(targetRadius > self.max_radius):
                scale *= self.max_radius / targetRadius
            return [ScaleInPlace(self.all, scale), ApplyMethod(self.all.move_to, self.aligned_edge)], scale
        else:
            return 0, self.scale

    def shrink_if_cross_border(self, node, offset_x=0):
        # Check if crossing bottom
        if node.get_bottom()[1] < self.ll[1]:
            scale = self.crossing_bottom_border(node)
        else:
            scale = 10e9

        # Check if crossing left and right borders
        if node.get_left()[0] - offset_x < self.ll[0]:
            scale = self.crossing_left_right_border(node, offset_x, left=True, scale=scale)

        if node.get_right()[0] + offset_x> self.lr[0]:
            scale = self.crossing_left_right_border(node, offset_x, left=False, scale=scale)

        if scale == 10e9:
                return 0, self.scale
        else :
                return [ScaleInPlace(self.all, scale), ApplyMethod(self.all.move_to, self.aligned_edge)], scale


    def check_overlapping_children(self, node):
        if node is None:
            return [], self.scale

        if node.left is not None and node.right is not None:
            overlap_x = node.left.all.get_right()[0] - node.right.all.get_left()[0] + self.margin[0] * 2
            if overlap_x > 0:
                offset_x = overlap_x / 2
                leftOffset = LEFT * offset_x
                rightOffset = RIGHT * offset_x
                newRline = Line(node.circle.point_at_angle(np.deg2rad(315)),
                                np.add(node.right.circle.point_at_angle(np.deg2rad(90)), rightOffset),
                                color=node.line_color)
                newLline = Line(node.circle.point_at_angle(np.deg2rad(225)),
                                np.add(node.left.circle.point_at_angle(np.deg2rad(90)), leftOffset),
                                color=node.line_color)

                animations = [
                    ApplyMethod(node.left.all.move_to, np.add(node.left.all.get_center(), leftOffset)),
                    Transform(node.lline, newLline),
                    ApplyMethod(node.right.all.move_to, np.add(node.right.all.get_center(), rightOffset)),
                    Transform(node.rline, newRline),
                ]

                shrink, scale = self.shrink_if_cross_border(self.root.all, offset_x)
                if shrink:
                    animations.extend(shrink)

                return animations, scale
            else:
                return [], self.scale

        animations, leftScale = self.check_overlapping_children(node.left)
        rightAnim, rightScale = self.check_overlapping_children(node.right)
        animations.extend(rightAnim)

        return animations, min(leftScale, rightScale)